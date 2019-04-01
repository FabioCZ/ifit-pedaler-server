package com.gottlicher.pedal4me

import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_scanner.*
import kotlinx.android.synthetic.main.scanner_item.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.DividerItemDecoration


class ScannerActivity : Activity() {

    val TAG:String = "ScannerActivity"

    val discoveredItems:ArrayList<Pair<String,String>> = ArrayList()
    val adapter:ScannerListAdapter = ScannerListAdapter(discoveredItems) { onSelected(it) }
    lateinit var nsdManager: NsdManager
    lateinit var discoveryListener:NsdManager.DiscoveryListener
    val retryCounter:HashMap<String, Int>  = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        discoveredItems.add(Pair("Test me", "123.4.5.6"))
        val lm = LinearLayoutManager(this)
        scanner_list.layoutManager = lm
        scanner_list.adapter = adapter

        val dividerItemDecoration = DividerItemDecoration(
            scanner_list.context,
            lm.orientation
        )
        scanner_list.addItemDecoration(dividerItemDecoration)
    }

    override fun onResume() {
        super.onResume()
        startDiscovery()
    }

    override fun onPause() {
        super.onPause()
        nsdManager.stopServiceDiscovery(discoveryListener)
    }

    private fun onSelected(ip:String){
        val resultIntent = Intent()
        resultIntent.putExtra("IP", ip)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    fun makeListener(man:NsdManager): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                val name = serviceInfo?.serviceName
                if (retryCounter.containsKey(name) && retryCounter[name]!! > 3) {
                    Log.d(TAG, "Failed to resolve, $name, not retrying")
                    return
                }

                if (!retryCounter.containsKey(serviceInfo?.serviceName)) {
                    retryCounter[serviceInfo?.serviceName!!] = 0
                }
                retryCounter[serviceInfo?.serviceName!!] = retryCounter[serviceInfo.serviceName]!! + 1
                Log.d(TAG, "Failed to resolve, $name, retrying, attempt: ${retryCounter[serviceInfo.serviceName]!!}")

                man.resolveService(serviceInfo,makeListener(man))
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                GlobalScope.launch(Dispatchers.Main) {
                    Log.d(TAG, "Resolved: ${serviceInfo?.serviceName}, ip: ${serviceInfo?.host?.hostAddress}")

                    if(discoveredItems.any { it.second == serviceInfo?.host?.hostAddress }) {
                        return@launch
                    }

                    val info = Pair(serviceInfo!!.serviceName, serviceInfo.host.hostAddress)
                    discoveredItems.add(info)
                    adapter.notifyItemRangeInserted(discoveredItems.size - 1, 1)
                }
            }

        }
    }
    private fun startDiscovery(){
        // Instantiate a new DiscoveryListener
        nsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager)
        discoveryListener = object : NsdManager.DiscoveryListener {

            // Called as soon as service discovery begins.
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success $service")
                nsdManager.resolveService(service, makeListener(nsdManager))
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost: $service")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }

        //"_services._dns-sd._udp"
        nsdManager.discoverServices("_http._tcp",NsdManager.PROTOCOL_DNS_SD, discoveryListener)

    }

}

class ScannerListAdapter (private val items:ArrayList<Pair<String,String>>, private val callback: (String) -> Unit) : RecyclerView.Adapter<ScannerListAdapter.ScannerListViewHolder> () {
    override fun onBindViewHolder(holder: ScannerListViewHolder, position: Int) {
        holder.itemView.nameTextView.text = items[position].first
        holder.itemView.ipTextView.text = items[position].second
        holder.itemView.onClick { holder.callback (items[position].second) }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannerListViewHolder {
        val li = LayoutInflater.from(parent.context)
        val view = li.inflate(R.layout.scanner_item,parent, false)
        return ScannerListViewHolder (view, callback)
    }

    class ScannerListViewHolder(view: View,
                                val callback: (String) -> Unit) : RecyclerView.ViewHolder(view)
}
