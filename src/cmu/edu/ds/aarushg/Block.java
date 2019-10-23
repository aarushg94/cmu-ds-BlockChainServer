/**
 *
 * AuthorID: aarushg
 * AuthorName: Aarush Gupta
 *
 * Block Class
 *
 * This class represents a simple Block.
 * Each Block object has an index - the position of the block on the chain, a timestamp - it holds the time of the
 * block's creation, data - a String holding the block's single transaction details, previousHash - the SHA256 hash
 * of a block's parent, a nonce - a BigInteger value determined by a proof of work routine.
 */

package cmu.edu.ds.aarushg;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;

public class Block {

    int index;
    Timestamp timestamp;
    String data;
    int difficulty;
    String previousHash;
    BigInteger nonce = new BigInteger("0");

    /**
     * Constructor for Block class to initialize a block object.
     * @param index
     * @param data
     * @param difficulty
     */

    public Block(int index, String data, int difficulty, Timestamp timestamp) {
        this.index = index;
        this.timestamp = timestamp;
        this.data = data;
        this.difficulty = difficulty;
    }

    /**
     * Method: calculateHash()
     * This method computes a hash of the concatenation of the index, timestamp, data, previousHash, nonce, and
     * difficulty. Static getInstance method is called with hashing SHA. Digest method is then called to calculate
     * the message digest of an input and return an array of byte. Encoded hexademical is then saved in a string.
     */

    public String calculateHash() {
        String hashGenerated = Integer.toString(this.getIndex()) + this.getTimestamp() + this.getData() + this.getPreviousHash() + this.getNonce() + this.getDifficulty();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(hashGenerated.getBytes());
            String hashToString = DatatypeConverter.printHexBinary(messageDigest);
            return hashToString;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception thrown for" + e);
        }
        return null;
    }

    /**
     * Method: proofOfWork()
     * The proof of work methods finds a good hash. It increments the nonce until it produces a good hash. Calls
     * calculateHash() method to compute the hash of the concatenation of the index, timestamp, data, previousHash,
     * nonce, and difficulty. If the hash has the appropriate number of leading hex zeroes, it is done and returns
     * that proper hash. If the hash does not have the appropriate number of leading hex zeroes, it increments
     * the nonce by 1 and tries again. It continues this process until it gets a good hash.
     */

    public String proofOfWork() {
        String hashValue = this.calculateHash();
        BigInteger a = BigInteger.ONE;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        while (!hashValue.substring(0, difficulty).equals(hashTarget)) {
            this.nonce = this.nonce.add(a);
            hashValue = this.calculateHash();
        }
        return hashValue;
    }

    /**
     * Method: toString()
     * Override the toString method to return the block details to be shown on the console to the user.
     */

    @Override
    public String toString() {
        return "{" +
                "\"index\" : " + index +
                ", \"time stamp\" : " + "\"" + timestamp + "\"" +
                ", \"Tx\" : " + "\"" + data + "\"" +
                ", \"PrevHash\" : " + "\"" + previousHash + "\"" +
                ", \"nonce\" : " + nonce +
                ", \"difficulty\" : " + difficulty +
                "}";
    }

    /**
     * Getters and setters
     */

    public int getIndex() {
        return index;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getData() {
        return data;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }
}