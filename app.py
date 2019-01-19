#!/usr/bin/env python3

from flask import Flask, render_template, request
from threading import Thread
from time import sleep
import math
import json
import os
os.environ['GPIOZERO_PIN_FACTORY'] = os.environ.get('GPIOZERO_PIN_FACTORY', 'mock')
import gpiozero

app = Flask(__name__)

bcmPin = 0
rpm = 0

pedalThread = None
keepRunning = True

def runPedalLoop():
	wire =  gpiozero.LED(int(bcmPin))
	latestBcm = bcmPin
	while keepRunning:
		if latestBcm != bcmPin:
			wire =gpiozero.LED(bcmPin)
		latestBcm = bcmPin
		delaySec = rpmToSecDelay()
		print('running at ' + str(rpm) + ' rpm (delay ' + str(delaySec) + ' sec) on pin ' + str(bcmPin))
		wire.on()
		sleep(delaySec)
		if not keepRunning:
			return
		wire.off()
		sleep(delaySec)
		if not keepRunning:
			return

def rpmToSecDelay():
	return (14110.1/math.pow(rpm,0.988567318803339))/1000.0


@app.route('/')
def homePage():
	bcmPin = request.args.get('bcmPin', 17)
	rpm = request.args.get('rpm', 45)
	return render_template('index.html', bcmPin=bcmPin, rpm=rpm)

@app.route('/set', methods=['POST'])
def set():
	global pedalThread
	global bcmPin
	global rpm
	reqData = request.get_json (force=True)
	bcmPin = int(reqData['bcmPin'])
	rpm = int(reqData['rpm'])

	if pedalThread is None:
		pedalThread = Thread(target = runPedalLoop)
		pedalThread.start()
		keepRunning = True
	return json.dumps({'success':True}), 200, {'ContentType':'application/json'} 

@app.route('/stop', methods=['POST'])
def stop():
	global pedalThread
	global keepRunning
	keepRunning = False
	pedalThread = None
	return json.dumps({'success':True}), 200, {'ContentType':'application/json'} 

