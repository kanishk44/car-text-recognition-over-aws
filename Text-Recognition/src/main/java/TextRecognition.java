import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.*;
import java.io.*;

public class TextRecognition {

    public static void main(String[] args) {

        String bucket = "njit-cs-643";
        String queue = "objects.fifo";

        S3Client s3BucketCli = S3Client.builder().region(Region.US_EAST_1).build();
        RekognitionClient rekogCli = RekognitionClient.builder().region(Region.US_EAST_1).build();
        SqsClient queueCli = SqsClient.builder().region(Region.US_EAST_1).build();
        carTextRecognize(bucket, queue, s3BucketCli, rekogCli, queueCli);
    }

    public static void carTextRecognize(String bucketName, String queueName, S3Client s3Cli, RekognitionClient rekCli,
                                        SqsClient sqsCli) {
        boolean queue = false;
        while (queue == false) {
            ListQueuesRequest requestList = ListQueuesRequest.builder().queueNamePrefix(queueName).build();
            ListQueuesResponse queueList = sqsCli.listQueues(requestList);
            if (queueList.queueUrls().size() > 0)
                queue = true;
        }

        String queueURL = "";
        try {
            GetQueueUrlRequest getReqQ = GetQueueUrlRequest.builder().queueName(queueName).build();
            queueURL = sqsCli.getQueueUrl(getReqQ).queueUrl();
        } catch (QueueNameExistsException exc) {
            throw exc;
        }

        try {
            boolean front = false;
            HashMap<String, String> resSet = new HashMap<>();

            while (!front) {

                ReceiveMessageRequest MsgReqRx = ReceiveMessageRequest.builder().queueUrl(queueURL).maxNumberOfMessages(1).build();
                List<Message> messages = sqsCli.receiveMessage(MsgReqRx).messages();

                if (messages.size() > 0) {
                    Message msg = messages.get(0);
                    String label = msg.body();

                    if (label.equals("-1")) {
                        front = true;
                    } else {
                        System.out.println("Recognizing text for cars from S3 bucket: " + label);
                        Image img = Image.builder().s3Object(S3Object.builder().bucket(bucketName).name(label).build()).build();
                        DetectTextRequest request = DetectTextRequest.builder().image(img).build();
                        DetectTextResponse result = rekCli.detectText(request);
                        List<TextDetection> textDetections = result.textDetections();

                        if (textDetections.size() != 0) {
                            String text = "";
                            for (TextDetection textDetection : textDetections) {
                                if (textDetection.type().equals(TextTypes.WORD))
                                    text = text.concat(" " + textDetection.detectedText());
                            }
                            resSet.put(label, text);
                        }
                    }

                    DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder().queueUrl(queueURL).receiptHandle(msg.receiptHandle()).build();
                    sqsCli.deleteMessage(deleteMessageRequest);
                }
            }
            try {
                FileWriter write = new FileWriter("output.txt");
                Iterator<Map.Entry<String, String>> itr = resSet.entrySet().iterator();
                while (itr.hasNext()) {
                    Map.Entry<String, String> pair = itr.next();
                    write.write(pair.getKey() + ":" + pair.getValue() + "\n");
                    itr.remove();
                }
                write.close();
                System.out.println("Output produced in output.txt");
            } catch (IOException exc) {
                System.out.println("Something went wrong!");
                exc.printStackTrace();
            }
        } catch (Exception exc) {
            System.err.println(exc.getLocalizedMessage());
            System.exit(1);
        }
    }
}