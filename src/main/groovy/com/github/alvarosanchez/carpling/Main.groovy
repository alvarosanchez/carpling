package com.github.alvarosanchez.carpling

import com.gmongo.GMongo
import com.mongodb.DB
import com.mongodb.DBCollection
import com.mongodb.MongoURI
import geb.Browser
import geb.navigator.Navigator
import groovy.util.logging.Log4j
import groovy.util.logging.Log4j2

import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Created by mariscal on 26/01/14.
 */
@Log4j
class Main {

    static void main(final String[] args) {
        new Main().run()
    }

    DB db
    DBCollection roundTrip
    DBCollection oneWay
    DBCollection returnTrip

    List<TrainTrip> foundRoundTrip = []
    List<TrainTrip> foundOneWay = []
    List<TrainTrip> foundReturn = []

    def browser = new Browser()

    Main() {
        connectDB()
        this.roundTrip = db.roundTrip
        this.oneWay = db.oneWay
        this.returnTrip = db.returnTrip
        browser.baseUrl = "http://www.carpling.com/es/"
    }


    def run() {
        try {
            browser.with {
                log.info "Navigating to Carpling home page..."
                go()

                log.info "Authenticating..."
                $('#login_form').user_email_login = System.getenv('carpling_email')
                $('#login_form').user_password_login = System.getenv('carpling_password')
                $('#login').click()

                go "train/compro-billetes-de-tren-ave"

                waitFor {
                    $('#route_form').displayed && $('#return_flag-1').displayed
                }

                log.info "Filling in search form..."
                $('#route_form').f_location = '30'
                $('#route_form').t_location = '28'
                $('#return_flag-1').value('1')

                $('#save_search').click()

                waitFor{
                    $('#onsale_counts h3 a').displayed
                }

                log.info "Parsing round trip tickets"
                $('tr', id: contains('ec_tr_both')).each {
                    storeTrip(getTrip(it), this.roundTrip, this.foundRoundTrip)
                }

                log.info "Parsing one way tickets"
                $('tr', id: contains('ec_tr_go')).each {
                    storeTrip(getTrip(it), this.oneWay, this.foundOneWay)
                }

                log.info "Parsing return tickets"
                $('tr', id: contains('ec_tr_back')).each {
                    storeTrip(getTrip(it), this.returnTrip, this.foundReturn)
                }

            }

            sendMail(System.getenv('email_to').split(','))

            browser.close()
            log.info "All done!"
            System.exit(0)
        } catch (e) {
            log.error "OOPS"
            e.printStackTrace()
            System.exit(-1)
        }
    }

    TrainTrip getTrip(Navigator tr) {
        def tripDate = tr.find('td').first().text()
        def description = tr.find('td')[1].text()
        new TrainTrip(tripDate: tripDate, description: description)
    }

    void storeTrip(TrainTrip trip, DBCollection dbCollection, List<TrainTrip> foundTrips) {
        if (!dbCollection.findOne(tripDate: trip.tripDate)) {
            dbCollection.insert(tripDate: trip.tripDate, description: trip.description)
            foundTrips << trip
        }
    }

    void connectDB() {
        def config = [
            server: System.getenv('mongo_server'),
            port: System.getenv('mongo_port'),
            database: "carpling",
            username: System.getenv('mongo_username'),
            password: System.getenv('mongo_password')
        ]

        log.info "Connecting to Mongo DB..."
        def uri = "mongodb://${config.username}:${config.password}@${config.server}:${config.port}/${config.database}"
        GMongo mongo = new GMongo(new MongoURI(uri))
        this.db = mongo.getDB(config.database)
        db.authenticate(config.username, config.password.toCharArray())

        log.info "... connected!"
    }

    void sendMail(String[] to){
        if (this.foundRoundTrip || this.foundOneWay || this.foundReturn) {
            def props = new Properties()
            props.put("mail.smtps.auth", "true")

            def session = Session.getDefaultInstance(props, null)

            def msg = new MimeMessage(session)

            msg.setSubject 'Carpling'
            msg.setFrom(new InternetAddress(System.getenv('email_username'), "Alvaro Sanchez-Mariscal"))
            msg.setContent buildMessage(), "text/html; charset=utf-8"
            to.each { String email ->
                msg.addRecipient Message.RecipientType.TO, new InternetAddress(email, email)
            }

            def transport = session.getTransport "smtps"

            def host = 'smtp.gmail.com'
            def username = System.getenv('email_username')
            def password = System.getenv('email_password')

            try {
                log.info "Sending email..."
                transport.connect (host, username, password)
                transport.sendMessage (msg, msg.getAllRecipients())
            }
            catch (Exception e) {
                log.error "OOPS"
                e.printStackTrace()
            }
        } else {
            log.info "Nothing new found. Not sending any email"
        }
    }

    String buildMessage() {
        String message = "No he encontrado nada nuevo desde la última vez."
        if (this.foundRoundTrip || this.foundOneWay || this.foundReturn) {
            message = """Hola,

¡He econtrado billetes nuevos! Recuerda contactar con los usuarios en
http://www.carpling.com/es/train/compro-billetes-de-tren-ave

<h2>Ida y vuelta</h2>
${buildTripCollection(this.foundRoundTrip)}

<h2>Ida</h2>
${buildTripCollection(this.foundOneWay)}

<h2>Vuelta</h2>
${buildTripCollection(this.foundReturn)}
"""
        }
        return message
    }

    String buildTripCollection(List<TrainTrip> foundTrips) {
        String text =  """
<h3>${foundTrips.size()} tickets</h3>

"""

        foundTrips.each {TrainTrip trip ->

            text += """
<strong>Fecha</strong>: ${trip.tripDate}<br/>
${trip.description}
<hr/>
"""

        }

        return text


    }
}
