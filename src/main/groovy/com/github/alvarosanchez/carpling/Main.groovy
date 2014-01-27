package com.github.alvarosanchez.carpling

import com.mongodb.DBCollection
import geb.Browser

import com.gmongo.GMongo
import com.mongodb.MongoURI
import com.mongodb.DB

import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Created by mariscal on 26/01/14.
 */
class Main {

    static void main(final String[] args) {
        try {

            DB db = connectDB()
            DBCollection roundTrip = db.roundTrip
            DBCollection oneWay = db.oneWay
            DBCollection returnTrip = db.returnTrip

            def foundTrips = []

            def browser = new Browser()
            browser.baseUrl = "http://www.carpling.com/es/"

            browser.with {
                go()

                $('#login_form').user_email_login = System.getProperty('carpling.email')
                $('#login_form').user_password_login = System.getProperty('carpling.password')

                $('#login').click()

                go "train/compro-billetes-de-tren-ave"

                waitFor {
                    $('#route_form').displayed
                }

                $('#route_form').f_location = '30'
                $('#route_form').t_location = '28'
                $('#return_flag-1').value('1')

                $('#save_search').click()

                waitFor{
                    $('#onsale_counts h3 a').displayed
                }

                println "Ida y vuelta"
                $('tr', id: contains('ec_tr_both')).each {
                    def tripDate = it.find('td').first().text()
                    def description = it.find('td')[1].text()
                    def trip = [tripDate: tripDate, description: description]
                    if (!roundTrip.findOne(tripDate: tripDate)) {
                        roundTrip.insert(trip)
                        foundTrips << trip
                    }

                }

                println "Ida"
                $('tr', id: contains('ec_tr_go')).each {
                    def tripDate = it.find('td').first().text()
                    def description = it.find('td')[1].text()
                    def trip = [tripDate: tripDate, description: description]
                    if (!oneWay.findOne(tripDate: tripDate)) {
                        oneWay.insert(trip)
                        foundTrips << trip
                    }
                }

                println "Vuelta"
                $('tr', id: contains('ec_tr_back')).each {
                    def tripDate = it.find('td').first().text()
                    def description = it.find('td')[1].text()
                    def trip = [tripDate: tripDate, description: description]
                    if (!returnTrip.findOne(tripDate: tripDate)) {
                        returnTrip.insert(trip)
                        foundTrips << trip
                    }
                }

            }

            sendMail(System.getProperty('email.to').split(','), 'Carpling', foundTrips.toString())

            browser.close()
            System.exit(0)
        } catch (e) {
            e.printStackTrace()
            System.exit(-1)
        }
    }

    static DB connectDB() {
        def config = [
                server: System.getProperty('mongo.server'),
                port: System.getProperty('mongo.port'),
                database: "carpling",
                username: System.getProperty('mongo.username'),
                password: System.getProperty('mongo.password')
        ]

        def uri = "mongodb://${config.username}:${config.password}@${config.server}:${config.port}/${config.database}"
        GMongo mongo = new GMongo(new MongoURI(uri))
        DB db = mongo.getDB(config.database)
        db.authenticate(config.username, config.password.toCharArray())

        return db
    }

    static void sendMail(String[] to, String subject, String message){
        def props = new Properties()
        props.put("mail.smtps.auth", "true")

        def session = Session.getDefaultInstance(props, null)

        def msg = new MimeMessage(session)

        msg.setSubject subject
        msg.setText message
        to.each { String email ->
            msg.addRecipient Message.RecipientType.TO, new InternetAddress(email, email)
        }

        def transport = session.getTransport "smtps"

        def host = 'smtp.gmail.com'
        def username = System.getProperty('email.username')
        def password = System.getProperty('email.password')

        try {
            transport.connect (host, username, password)
            transport.sendMessage (msg, msg.getAllRecipients())
        }
        catch (Exception e) {
            e.printStackTrace()
        }
    }
}
