#!/usr/bin/env bash
CURR=`pwd`
export FLASK_APP=$CURR/app.py
export FLASK_ENV=production
flask run --host=0.0.0.0 --port=80