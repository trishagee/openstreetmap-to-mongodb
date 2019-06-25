import com.mongodb.MongoClient
import org.bson.Document

def mongoClient = new MongoClient()
def collection = mongoClient.getDatabase('Cafelito').getCollection('Test')

collection.insertOne(new Document(['name': 'Trisha']))

println "Collection contains ${collection.countDocuments()} documents"

