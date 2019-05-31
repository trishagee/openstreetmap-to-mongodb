/**
 * Converts XML from OpenStreetMap into Map data structures and inserts into MongoDB. The XML comes from the
 * Overpass XAPI http://www.overpass-api.de/api/xapi.  More information on the original version of this script
 * here: http://trishagee.github.io/post/groovy_import_to_mongodb/
 */

import com.mongodb.MongoClient
import com.mongodb.client.model.Indexes
import org.bson.Document

import java.util.zip.ZipFile

def mongoClient = new MongoClient()
def collection = mongoClient.getDatabase('Cafelito').getCollection('CoffeeShop')
// NOTE: This script drops the whole collection before reimporting it
collection.drop()

//NOTE: this requires the correct working directory (src/main) in the run configuration
def coffeeShops = new ZipFile(new File('resources/all-coffee-shops-2019.xml.zip'))
def xmlSlurper = new XmlSlurper().parse(coffeeShops.getInputStream(coffeeShops.getEntry('all-coffee-shops-2019.xml')))
xmlSlurper.node.findAll { it.tag.any { it.@k.text() == 'name' } }
               .each {
                   def coffeeShop = [openStreetMapId: it.@id.text(),
                                     location       : [coordinates: [it.@lon, it.@lat]*.text()*.toDouble(),
                                                       type       : 'Point']]
                   it.tag.findAll { isValidFieldName(it.@k.text()) }
                         .each {
                             coffeeShop.put(it.@k.text(), it.@v.text())
                         }
                   collection.insertOne(new Document(coffeeShop))
               }

println "\nTotal imported: " + collection.countDocuments()

collection.createIndex(Indexes.geo2dsphere('location', '2dsphere'))

private static boolean isValidFieldName(fieldName) {
    !fieldName.contains('.') && !(fieldName == 'location')
}
