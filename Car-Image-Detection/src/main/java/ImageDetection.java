import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.*;

public class ImageDetection {
    public static void main(String[] args) {
        String bucket = "njit-cs-643";
        String queue = "objects.fifo";
        String queueGroup = "queueGrp";
        S3Client s3BucketCli = S3Client.builder().region(Region.US_EAST_1).build();
        RekognitionClient rekogCli = RekognitionClient.builder().region(Region.US_EAST_1).build();
        SqsClient queueCli = SqsClient.builder().region(Region.US_EAST_1).build();
        detectImg(bucket, queue, queueGroup, s3BucketCli, rekogCli, queueCli);
    }

    public static void detectImg(String bucketName, String queueName, String queueGroup, S3Client s3Cli,
                                           RekognitionClient rekCli, SqsClient sqsCli) {
        String queueURL = "";
        try {
            ListQueuesRequest queRequest = ListQueuesRequest.builder().queueNamePrefix(queueName).build();
            ListQueuesResponse queResponse = sqsCli.listQueues(queRequest);

            if (queResponse.queueUrls().size() == 0) {
                CreateQueueRequest request = CreateQueueRequest.builder().attributesWithStrings(Map.of("FifoQueue", "true", "ContentBasedDeduplication", "true")).queueName(queueName).build();
                sqsCli.createQueue(request);
                GetQueueUrlRequest urlQueue = GetQueueUrlRequest.builder().queueName(queueName).build();
                queueURL = sqsCli.getQueueUrl(urlQueue).queueUrl();
            } else {
                queueURL = queResponse.queueUrls().get(0);
            }
        } catch (QueueNameExistsException exc) {
            throw exc;
        }

        try {
            ListObjectsV2Request listObjRequest = ListObjectsV2Request.builder().bucket(bucketName).maxKeys(10).build();
            ListObjectsV2Response listObjResponse = s3Cli.listObjectsV2(listObjRequest);

            for (S3Object obj : listObjResponse.contents()) {
                System.out.println("Image received in NJIT-cs-643 S3 bucket: " + obj.key());

                Image img = Image.builder().s3Object(software.amazon.awssdk.services.rekognition.model.S3Object
                                .builder().bucket(bucketName).name(obj.key()).build()).build();
                DetectLabelsRequest request = DetectLabelsRequest.builder().image(img).minConfidence((float) 90).build();
                DetectLabelsResponse result = rekCli.detectLabels(request);
                List<Label> labels = result.labels();

                for (Label label : labels) {
                    if (label.name().equals("Car")) {
                        sqsCli.sendMessage(SendMessageRequest.builder().messageGroupId(queueGroup).queueUrl(queueURL)
                                .messageBody(obj.key()).build());
                        break;
                    }
                }
            }

            sqsCli.sendMessage(SendMessageRequest.builder().queueUrl(queueURL).messageGroupId(queueGroup).messageBody("-1").build());
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }
}