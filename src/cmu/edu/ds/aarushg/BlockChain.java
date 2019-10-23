package cmu.edu.ds.aarushg;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class BlockChain {

    ArrayList<Block> blockChain;
    String chainHash;

    /**
     * Constructor for BlockchainServer class.
     */

    public BlockChain() {
        this.blockChain = new ArrayList<>();
        this.chainHash = "";
    }

    /**
     * Method: addTransaction()
     * Asks for user input for difficulty, transaction details. It then creates a new block and passes all the user
     * input data as parameters of the block object and add block b to the blockchain. The total time it took to add
     * a block is also calculated in milliseconds.
     * @param index - index of block
     * @param difficulty - user input value for difficulty
     * @param transaction - user input value for transaction details
     * @return
     */

    public String addTransaction(int index, int difficulty, String transaction) {
        long startTime = System.currentTimeMillis();
        Block b = new Block(index, transaction, difficulty, getTime());
        addBlock(b);
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        return "Total execution time to add this block was " + elapsedTime + " milliseconds";
    }

    /**
     * Method : addBlock()
     * Takes in an object of type Block and adds to the BlockChain as the most recent block.
     * @param b --> object of class B
     */

    public void addBlock(Block b) {
        String previousHash = "";
        if (blockChain.size() == 0) {
            previousHash = "";
        } else {
            previousHash = getLatestBlock().proofOfWork();
        }
        b.previousHash = previousHash;
        chainHash = b.proofOfWork();
        blockChain.add(b);
    }

    /**
     * Method: corruptBlockChain()
     * Asks block ID from user to corrupt that block. Asks for transaction details to be overwritten on that block.
     * Overwrites the corrupt data at that block.
     * @param difficulty -> Block ID value input by the user
     * @param transaction -> Transaction value for the block ID input by the user
     * @return
     */

    public String corruptBlockChain(int difficulty, String transaction) {
        int blockChainIndex = difficulty;
        String corruptInput;
        corruptInput = transaction;
        if (difficulty >= blockChain.size()) {
            return "Block ID doesn't exist";
        } else {
            blockChain.get(blockChainIndex).setData(corruptInput);
            return "Block " + blockChainIndex + " now holds " + corruptInput;
        }
    }

    /**
     * Method: hashesPerSecond()
     * Computes the hashes per second on a simple String "00000000". Saves current time in milliseconds. Static
     * getInstance method is called with hashing SHA. Digest method is then called to calculate the message
     * digest of an input and return an array of byte. Encoded hexademical is then saved in a string. hashCount
     * is incremented till current time reaches 1 second.
     */

    public int hashesPerSecond() {
        int hashCount = 0;
        String simpleString = "00000000";
        long endTime = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < endTime) {
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                System.out.println("message : " + e);
            }
            byte[] messageDigest = md.digest(simpleString.getBytes());
            String hashedSimpleString = DatatypeConverter.printHexBinary(messageDigest);
            hashCount++;
        }
        return hashCount;
    }

    /**
     * Method: repairChain()
     * This method repairs the chain. It checks the hashes of each block and ensures that any illegal hashes are
     * recomputed. After this method is run, the chain will be valid. The routine does not modify any difficulty
     * values. It computes new proof of work based on the difficulty specified in the Block.
     */

    public void repairChain() {
        for (int i = 1; i < blockChain.size(); i++) {
            Block previousBlock = blockChain.get(i - 1);
            Block currentBlock = blockChain.get(i);
            String previousProofOfWork = previousBlock.proofOfWork();
            String hashTarget = new String(new char[previousBlock.getDifficulty()]).replace('\0', '0');
            if (!previousProofOfWork.substring(0, previousBlock.getDifficulty()).equals(hashTarget) || !previousBlock.proofOfWork().equals(currentBlock.getPreviousHash())) {
                previousBlock.setDifficulty(previousBlock.getDifficulty());
                currentBlock.setPreviousHash(previousBlock.proofOfWork());
                previousBlock.proofOfWork();
            }
        }
        chainHash = getLatestBlock().proofOfWork();
    }

    /**
     * Method: viewBlockChainStatus()
     * Displays the details on the current blockchain such that size of blockchain, no. of hashes per second,
     * difficulty and nonce value for each block of the blockchain.
     *
     * JSON Parsing Source: https://stackoverflow.com/questions/8876089/how-to-fluently-build-json-in-java
     *
     * @return
     */

    public org.json.JSONObject viewBlockChainStatus() {
        org.json.JSONObject jsonString = new org.json.JSONObject().put("size", getChainSize())
                .put("hashes", hashesPerSecond())
                .put("difficulty", getLatestBlock().getDifficulty())
                .put("nonce", getLatestBlock().getNonce().toString())
                .put("chainHash", chainHash);
        return jsonString;
    }

    /**
     * Method: isChainValid()
     *
     * Scenario 1 --> BlockchainServer only has 1 block i.e. Genesis block @ Position 0
     * This routine computes the hash of the block and checks that the hash has the requisite number of leftmost
     * 0's (proof of work) as specified in the difficulty field. It also checks that the chain hash is equal to
     * this computed hash. If either check fails, return false. Otherwise, return true.
     */

    public boolean isChainValid() {
        if (blockChain.size() == 1) {
            Block b = blockChain.get(0);
            String hash = b.proofOfWork();
            String hashTarget = new String(new char[b.difficulty]).replace('\0', '0');
            if (hash.substring(0, b.difficulty).equals(hashTarget) && chainHash.equals(hash)) {
                return true;
            }
        }

        /**
         * Scenario 2 --> BlockChain only more than 1 block.
         * We check block 1 and continue checking until we have validated the entire chain. The first check will
         * involve a computation of a hash in Block 0 and a comparison with the hash pointer in Block 1.
         * If they match and if the proof of work is correct, move to the next block in the chain. At the end,
         * it checks that the chain hash is also correct.
         */

        for (int i = 1; i < blockChain.size(); i++) {
            Block currentBlock = blockChain.get(i);
            Block previousBlock = blockChain.get(i - 1);
            String hashedPreviousBlock = previousBlock.proofOfWork();
            String pointer = currentBlock.getPreviousHash();
            if (!hashedPreviousBlock.equals(pointer)) {
                String hashTarget = new String(new char[previousBlock.difficulty]).replace('\0', '0');
                System.out.println("..Improper hash on node " + previousBlock.index + " Does not begin with " + hashTarget);
                return false;
            }
        }
        if (!chainHash.equals(getLatestBlock().proofOfWork())) {
            return false;
        }
        return true;
    }

    /**
     * Method: getLatestBlock()
     * Returns the most recently added Block.
     */

    public Block getLatestBlock() {
        int totalLength = blockChain.size();
        return this.blockChain.get(totalLength - 1);
    }

    /**
     * Method: getChainSize
     * Returns the size of the BlockChain.
     */

    public int getChainSize() {
        int totalLength = blockChain.size();
        return totalLength;
    }

    /**
     * Method: toString()
     * Returns the formatted BlockChain details output desired by the user.
     */

    @Override
    public String toString() {
        String toString = "{\"ds_chain\":[";
        for (int i = 0; i < blockChain.size(); i++) {
            if (i == blockChain.size() - 1) {
                toString += blockChain.get(i).toString() + "]";
            } else {
                toString += blockChain.get(i).toString() + ",";
            }
        }
        toString = toString + ",\"chainHash\":\"" + chainHash + "\"}";
        return toString;
    }

    /**
     * Method: getTime()
     * @return current time
     */

    public java.sql.Timestamp getTime() {
        return new Timestamp(System.currentTimeMillis());
    }
}