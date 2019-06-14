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

// NOTE: this requires the correct working directory (project_home/build/classes/groovy) in the run configuration if
// running from IntelliJ IDEA
def coffeeShops = new ZipFile(new File(getClass().getResource('all-coffee-shops-2019.xml.zip').getFile()))
println coffeeShops.getName() // if the script fails, it'll be because it can't find the file on the classpath

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
