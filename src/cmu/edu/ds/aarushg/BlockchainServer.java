/***
 * authorID: aarushg
 * authorName: Aarush Gupta
 *
 * BlockChain Server Class
 *
 * This program implements a TCP server. It receives the client request and checks if the public key hashes to
 * the correct ID and whether or not the request is correctly signed. Once both the checks have been performed
 * the server performs computations and sends back the results based on user selection.
 */

package cmu.edu.ds.aarushg;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class BlockchainServer {

    static String operationValue;
    static int difficulty;
    static String transaction;

    /***
     * main() method
     * Initializes socket, input and output streams. Starts the server and waits for a connection. Takes input from
     * client Socket, reads the message from the client via scanner's hasNextLine() method. Based on the data sent
     * it calls it's helper methods to do various transactions on the blockchain such as viewing the status, adding
     * a new transaction, viewing the blockchain itself, corrupting the chain, repairing the chain or quitting the
     * program after it verifies the identity of the client by new-ing up the BlockChain class object. After doing
     * the computations, it then sends this fetched data back to the client.
     *
     * This acts as a test driver for the BlockChain. It will begin by creating a BlockChain object and
     * then adding the Genesis block to the chain. The Genesis block will be created with an empty string as the
     * previous hash and a difficulty of 2.
     *
     * @param args
     */

    public static void main(String[] args) {

        int index = 0;
        BlockChain bc = new BlockChain();
        Block b = new Block(0, "Genesis", 2, bc.getTime());
        bc.addBlock(b);
        index++;
        Socket clientSocket = null;
        System.out.println("---Server running---");
        while (true) {
            try {
                int serverPort = 7777;
                ServerSocket listenSocket = new ServerSocket(serverPort);
                clientSocket = listenSocket.accept();
                Scanner scanner = new Scanner(clientSocket.getInputStream());
                PrintWriter out;
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));
                while (scanner.hasNextLine()) {
                    String data = scanner.nextLine();
                    Object obj = new JSONParser().parse(data);
                    JSONObject jo = (JSONObject) obj;
                    String clientID = (String) jo.get("clientID");
                    operationValue = (String) jo.get("operationValue");
                    difficulty = (int) (long) jo.get("difficulty");
                    transaction = (String) jo.get("transaction");
                    String signature = (String) jo.get("signature");
                    String e = (String) jo.get("BigIntegerE");
                    String n = (String) jo.get("BigIntegerN");
                    String key = clientID + e + n + operationValue + difficulty + transaction;
                    String eBigIntegerString = String.valueOf(e);
                    String nBigIntegerString = String.valueOf(n);
                    String newBigIntegerString = eBigIntegerString + nBigIntegerString;
                    String hash = ComputeSHA_256_as_Hex_String(newBigIntegerString);
                    String babyHash = null;
                    if (hash != null) {
                        babyHash = hash.substring(hash.length() - 40);
                    }
                    String hashedKey = ComputeSHA_256_as_Hex_String(key);

                    /***
                     * validatePKtoID -> true if public key correctly hashes to the ID.
                     */

                    boolean validatePKtoID = false;
                    if (clientID.equals(babyHash)) {
                        validatePKtoID = true;
                    }

                    /***
                     * validateSignature -> true if request is properly signed
                     */

                    boolean validateSignature = false;
                    try {
                        validateSignature = verify(hashedKey, signature, e, n);
                    } catch (Exception e1) {
                        System.out.println("In Exception 1");
                    }

                    /***
                     * If both the conditions stated above are true, check for user input and perform computations
                     * and return the result to the client else send error message and close the connection.
                     */

                    if (validatePKtoID && validateSignature) {
                        String responseString;
                        switch (Integer.parseInt(operationValue)) {
                            case 0:
                                out.println(bc.viewBlockChainStatus());
                                out.flush();
                                break;
                            case 1:
                                responseString = bc.addTransaction(index, difficulty, transaction);
                                index++;
                                out.println(responseString);
                                out.flush();
                                break;
                            case 2:
                                bc.isChainValid();
                                if (bc.isChainValid()) {
                                    responseString = "Chain Verification: true";
                                    out.println(responseString);
                                    out.flush();
                                } else {
                                    responseString = "Chain verification: false";
                                    out.println(responseString);
                                    out.flush();
                                }
                                break;
                            case 3:
                                responseString = bc.toString();
                                out.println(responseString);
                                out.flush();
                                break;
                            case 4:
                                responseString = bc.corruptBlockChain(difficulty, transaction);
                                out.println(responseString);
                                out.flush();
                                break;
                            case 5:
                                long startTime = System.currentTimeMillis();
                                bc.repairChain();
                                long endTime = System.currentTimeMillis();
                                long elapsedTime = endTime - startTime;
                                responseString = "Total execution time required to repair the chain was " + elapsedTime + " milliseconds";
                                out.println(responseString);
                                out.flush();
                                break;
                            case 6:
                                out.close();
                                break;
                            default:
                                System.out.println("Error In Request");
                                out.flush();
                                out.close();
                                break;
                        }
                    }
                }

                /***
                 * Handling socket, number format and I/O exceptions
                 */

            } catch (SocketException e) {
            } catch (NumberFormatException n) {
                System.out.println("Number format exception");
            } catch (IOException e) {
                System.out.println("IO Exception:" + e.getMessage());
            } catch (ParseException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    System.out.println("Socket: " + e.getMessage());
                }
            }
        }
    }

    /***
     * convertToHex() method
     * Source: StackOverflow + 'BabyHash' program
     * Converts a byte array to a String. Each nibble (4 bits) of the byte array is represented by a hex character.
     * @param data
     */

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /***
     * ComputeSHA_256_as_Hex_String() method
     * Source: 'BabyHash' class
     * Create a SHA256 digest. Initialize byte array for storing the hash. Perform the hash and store
     * the result. Handling exceptions.
     * @param text
     */

    public static String ComputeSHA_256_as_Hex_String(String text) {
        try {
            MessageDigest digest;
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes;
            digest.update(text.getBytes(StandardCharsets.UTF_8), 0, text.length());
            hashBytes = digest.digest();
            return convertToHex(hashBytes);
        } catch (NoSuchAlgorithmException nsa) {
            System.out.println("No such algorithm exception thrown " + nsa);
        }
        return null;
    }

    /***
     * hexStringToByteArray() method
     * Source: StackOverflow + 'BabySign'
     * @param s
     */

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /***
     * verify() method
     * Source: Modifications done to verify() method of 'BabyVerify'
     * Take the encrypted string and make it a BigInteger as encryptedHash. Decrypt the same as decryptedHash.
     * Convert to byte array. Create a new array to store 0 and add 0 as the most significant bit to make it
     * positive. Convert the same to a BigInteger and compare with decryptedHash. Inform the client on how to
     * compare.
     * @param messageToCheck
     * @param encryptedHashStr
     * @param eValue
     * @param nValue
     */

    public static boolean verify(String messageToCheck, String encryptedHashStr, String eValue, String nValue) throws Exception {
        BigInteger e = new BigInteger(eValue);
        BigInteger n = new BigInteger(nValue);
        BigInteger encryptedHash = new BigInteger(encryptedHashStr);
        BigInteger decryptedHash = encryptedHash.modPow(e, n);
        byte[] temp = hexStringToByteArray(messageToCheck);
        byte[] newTemp = new byte[temp.length + 1];
        newTemp[0] = 0;
        for (int i = 0; i < temp.length; i++) {
            newTemp[i + 1] = temp[i];
        }
        BigInteger tempBigInteger = new BigInteger(newTemp);
        return tempBigInteger.compareTo(decryptedHash) == 0;
    }

}