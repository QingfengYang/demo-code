package com.robert.hadoop.mtxx;

import com.robert.hadoop.kafka.ProducerCreator;
import org.apache.kafka.clients.producer.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * 出来秀秀社区事件数据写入kafka
 *
 */
public class PumpEventDataToKafka
{
    public static final String TOPIC = "xx_event_v1";
    public static void main( String[] args ) throws FileNotFoundException {
        if (args.length < 1) {
            System.err.println("Not any arguments found, exit");
            System.exit(1);
        }
        File eventData = new File(args[0]);

        Producer<String, String> producer = ProducerCreator.createKafkaProducer();

        Scanner scanner = new Scanner(eventData);
        int i = 0;
        while (scanner.hasNextLine()) {
            i ++;
            String line = scanner.nextLine();
            if (i == 1) {
                System.out.printf("Headers: %s\n", line);
            } else {
                System.out.printf("Line: %s: %s\n", i, line);
                producer.send(new ProducerRecord<String, String>(TOPIC, Integer.toString(i), line), new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        System.out.printf("Send complete meta: %s", recordMetadata.toString());
                    }
                });
            }
        }
        producer.close();
        scanner.close();
    }
}
