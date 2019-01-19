# ifit-pedaler-server

A very simple server that serves an HTML page to control simulated pedaling on a connected iFit console. Inspired by William Thomas's [https://github.com/williamt-ifit/ifitpedaler](https://github.com/williamt-ifit/ifitpedaler)

# Setup

* You will probably need to open port 80 on your raspberry pi. Simplest way to do that is via `ufw`:
```
sudo apt-get install ufw
sudo ufw allow 22 #so that you can still ssh into it
sudo ufw allow 80
sudo ufw enable
sudo reboot
```
* If you want to use another port, just edit the port parameter in `run.sh`
* You will need 2 jumper wires to go from your raspberry Pi's selected BCM port + ground to the console. On a bike console, it seems like you will generally want to connect the jumper wires to the two leftmost pins (ground + pos can be in either combination, so you might have to try both).


# Usage
* Enter in the BCM pin number that your wire is connected to
* Enter in (approximate) RPMs
* Press "Pedal!" to start pedaling
* Press "Stop" to stop pedaling.
* If you close the page without pressing stop, the server will keep on "pedaling"