#!/usr/bin/python

from app import app
from flask import jsonify 
from app import app
from cassandra.cluster import Cluster
import calendar 
import time
from flask import render_template


cluster = Cluster(["52.88.7.3","52.89.90.56","52.88.121.199","52.89.89.147"]) 
session = cluster.connect('text_bids') 

g_monitors = {}
   
@app.route('/api/email/<email>/<date>')
def get_email(email, date):
	stmt = "SELECT * FROM email WHERE id=%s and date=%s"
	response = session.execute(stmt, parameters=[email, date])
	response_list = []
	for val in response:
		response_list.append(val)
	jsonresponse = [{"first name": x.fname, "last name": x.lname, "id": x.id, "message": x.message, "time": x.time} for x in response_list]
	return jsonify(emails=jsonresponse)
	

@app.route('/')
@app.route('/index')
def index():
    return render_template("index.html")

@app.route('/monitors')
def monitors():
    return render_template("monitors.html")
	
@app.route('/api/stream/bidswon/<company>/<begin>/<end>', defaults={'begin': None, 'end':None})
def report_monitors(company, begin, end):
	print("begin: "+str(begin))
	stmt = "SELECT * from bidswon WHERE created_utc>"+str(begin)+" AND created_utc<"+str(end)+" AND pharmatag = '"+company+"';"
	print(stmt)
	# return stmt
	response = session.execute(stmt)
	# print response
	response_list = []
	for val in response:
		response_list.append(val)
	jsonresponse = [{"author": x.author, "price": x.price, "body": x.body} for x in response_list]
	return jsonify(stream=jsonresponse)
