



def sendkafka(messages):
    kafka = KafkaClient("localhost:9092")
    producer = SimpleProducer(kafka)
    for message in messages:
        yield producer.send_messages('topic', message)

sentRDD = messageRDD.mapPartitions(sendkafka)