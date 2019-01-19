#!/usr/bin/env bash
CURR=`pwd`
export FLASK_APP=$CURR/app.py
export FLASK_ENV=development
flask run --port=80