# car-text-recognition-over-aws

## Setting Up EC2 Instances for Text and Car Recognition
This guide provides step-by-step instructions for setting up two Amazon EC2 instances, EC2_A and EC2_B, to run text and car recognition code simultaneously.

## EC2 Setup
1. Log in to the AWS Management Console and click on "Services" -> "EC2".
2. In the "Instances" section, click on "Launch Instances" -> "Launch Instances".
3. Select the "Amazon Linux 2 AMI (HVM) - Kernel 5.10, SSD Volume Type - ami-0cff7528ff583bf9a (64-bit x86) / ami-00bf5f1c358708486 (64-bit Arm)".
4. Select the "t2.micro type (Free tier eligible)".
5. Click "Next: Configure Instance Details".
6. Ensure "Number of instances" is "1".
7. Click "Next: Add Storage".
8. Click "Next: Add Tags".
9. Insert "EC2_A" under "Key" and "Value" and “EC2_B” for the second one.
10. Click "Next: Configure Security Group".
11. Click "Add Rule" to include the following and it will autofill: SSH, HTTP, HTTPS.
12. Under 'Source' drop down for each rule, select 'My IP'.
13. Click "Review and Launch".
14. Click on "Launch" after reviewing your information.
15. On the dialog that pops up, select "Create a new key pair" in the drop down.
16. Name it "EC2_A_CS643" and “EC2_B_CS643” for 2nd EC2.
17. Hit "Download key pair."
18. Use the same keypair for 2nd EC2 aka EC2_B. Do not create a new one.
19. Hit "Launch Instances".
20. Hit "View Instances".

## Permissions Setup
1. Run the following command to set the correct permissions for the .pem file: $ chmod 400 EC2_A_CS643.pem.
2. Use Putty to connect to the instance.
3. After you have connected, run the following commands to update Java from 1.7 to 1.8 on the EC2: ```$ sudo yum install java-1.8.0-devel``` and ```$ sudo /usr/sbin/alternatives --config java```
4. Upon running the second command, you should enter the number that corresponds to java-1.8.0-openjdk.x86_64 (/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.322.b06-2.el8_5.x86_64/jre/bin/java).
5. You will need to complete this step on BOTH EC2's.


## Credentials Setup
1. Click on "Services" -> "IAM".
2. On the left tab side, under "Access Management" -> "Users".
3. Click on the user created.
4. Open the "Security credentials" tab, and then choose "Create access key".
5. To see the new access key, choose "Show". Your credentials resemble the following: Access key ID: AKIAQB2SHQLRR6NF57M5 and Secret access key: 73IWqwYi2nyPG0f16AnCjDARbBPK3zS6Uuts+p8i.
6. To download the key pair, choose 'Download .csv' file.
7. Store the .csv file with keys in a secure location.
8. Connect into both EC2's that you created in the previous step, and on each one, create a file:
  ```mkdir .aws $ touch .aws/credentials```
  ```$ vi .aws/credentials```
9. Paste the copied credentials into the credential file on each EC2 as follows:
    makefile Copy code [default] aws_access_key_id= THI5I5NOTTH3ACCE55K3Y
    aws_secret_access_key= THI5I5NOTTH3S3CR3TACCE55K3Y
Note: You will need to re-copy and paste these credentials onto both EC2's whenever they change (after your session ends).

## Java/Git Installation
1. If you do not have an up-to-date version of Java or Maven installed on your local machine, run the following commands locally:
`$ sudo amazon-linux-extras install java-openjdk11`, `$ java -version`, `$ alternatives --config java`. Now select the one which has JAVA 11.
2. Install git in your EC2's.
3. SSH into EC2-B and run the following command: `$ java -jar **locate the jar file for text recognition**`.
4. Now, both programs will be running simultaneously. The program on EC2-A is processing all images in the S3 bucket (njit-cs-643) and sending the indexes of images that include cars to EC2-B through SQS, which in turn is processing these images to find out which ones include text as well.



