from app import app
from flask import jsonify
from cassandra.cluster import Cluster

from flask import render_template

cluster = Cluster(['52.89.89.147','52.88.121.199','52.89.90.56','52.88.7.3'])

session = cluster.connect('playground')


@app.route('/')
@app.route('/index')
def index():
  user = { 'nickname': 'Miguel' } # fake user
  mylist = [1,2,3,4]
  return render_template("index.html", title = 'Home', user = user, mylist = mylist)

@app.route('/api/<email>/<date>')
def get_email(email, date):
	stmt = "SELECT * FROM email WHERE id=%s and date=%s"
	response = session.execute(stmt, parameters=[email, date])
	response_list = []
	for val in response:
	    response_list.append(val)
	jsonresponse = [{"first name": x.fname, "last name": x.lname, "id": x.id, "message": x.message, "time": x.time} for x in response_list]
	return jsonify(emails=jsonresponse)

@app.route('/email')
def email():
	return render_template("email.html")

@app.route("/email", methods=['POST'])
def email_post():
	emailid = request.form["emailid"]
	date = request.form["date"]
	stmt = "SELECT * FROM email WHERE id=%s and date=%s"
	response = session.execute(stmt, parameters=[emailid, date])
	response_list = []
	for val in response:
	 response_list.append(val)
	jsonresponse = [{"fname": x.fname, "lname": x.lname, "id": x.id, "message": x.message, "time": x.time} for x in response_list]
	return render_template("emailop.html", output=jsonresponse)


@app.route('/realtime')
def realtime():
	return render_template("realtime.html")

#we will now create emailop.html to display the results


