
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author olive_000
 */
public class Chord extends java.rmi.server.UnicastRemoteObject implements ChordMessageInterface {

   /**
    *
    */
   public static final int M = 2;

   Registry registry;    // rmi registry for lookup the remote objects.
   ChordMessageInterface successor;
   ChordMessageInterface predecessor;
   ChordMessageInterface[] finger;
   int nextFinger;
   int i;   		// GUID
   Timer timer = new Timer();

   /**
    *
    * @param ip the ip that the user is
    * @param port the port that chord is
    * @return
    */
   public ChordMessageInterface rmiChord(String ip, int port) {
      ChordMessageInterface chord = null;
      try {
         Registry registry = LocateRegistry.getRegistry(ip, port);
         chord = (ChordMessageInterface) (registry.lookup("Chord"));
         return chord;
      }
      catch (RemoteException e) {
         e.printStackTrace();
      }
      catch (NotBoundException e) {
         e.printStackTrace();
      }
      return null;
   }

   /**
    * Checks to see if the key is greater than key1 and at most key2
    *
    * @param key the key to check
    * @param key1 the lower bound
    * @param key2 the upper bound
    * @return boolean
    */
   public Boolean isKeyInSemiCloseInterval(int key, int key1, int key2) {
      if (key1 < key2) {
         return (key > key1 && key <= key2);
      }
      else {
         return (key > key1 || key <= key2);
      }
   }

   /**
    * see if the key is between but not including key1 key2
    *
    * @param key the key to check
    * @param key1 the lower bound
    * @param key2 the upper bound
    * @return boolean
    */
   public Boolean isKeyInOpenInterval(int key, int key1, int key2) {
      if (key1 < key2) {
         return (key > key1 && key < key2);
      }
      else {
         return (key > key1 || key < key2);
      }
   }

   /**
    * This function is to put files into folders
    *
    * @param guid the id of the folder to put in
    * @param data the data to put in
    * @throws IOException
    */
   public void put(int guid, byte[] data) throws IOException {
      String path = "./" + getId() + "/" + guid; // path to file
      File f = new File(path);
      FileOutputStream fis = new FileOutputStream(f);
      fis.write(data); // write data
      fis.close();
   }

   /**
    * This is to grab file from a specified folder and returns data
    *
    * @param guid grabs the file in specific folder
    * @return returns data
    * @throws IOException
    */
   public byte[] get(int guid) throws IOException {
      String path = "./" + getId() + "/" + guid; // path to file
      File f = new File(path);
      FileInputStream fis = null;
      fis = new FileInputStream(f);
      byte[] data = new byte[fis.available()];
      fis.read(data); // read data
      fis.close();
      return data;
   }

   /**
    * deletes a file
    *
    * @param guid the file path
    * @throws RemoteException
    */
   public void delete(int guid) throws RemoteException {
      String path = "./" + getId() + "/" + guid; // path to file
      File f = new File(path);
      if (f.delete()) {
         System.out.println(f.getName() + " deleted");
      }
      else {
         System.out.println("Failed to delete " + f.getName());
      }
   }

   /**
    * Returns id
    *
    * @return @throws RemoteException
    */
   public int getId() throws RemoteException {
      return i;
   }

   /**
    * see if the thing is alive return true
    *
    * @return @throws RemoteException
    */
   public boolean isAlive() throws RemoteException {
      return true;
   }

   /**
    * returns the predecessor
    *
    * @return @throws RemoteException
    */
   public ChordMessageInterface getPredecessor() throws RemoteException {
      return predecessor;
   }

   /**
    * finds the successor
    *
    * @param key the key of of the to be successor
    * @return
    * @throws RemoteException
    */
   public ChordMessageInterface locateSuccessor(int key) throws RemoteException {
      if (key == i) {
         throw new IllegalArgumentException("Key must be distinct that  " + i);
      }
      if (successor.getId() != i) {
         if (isKeyInSemiCloseInterval(key, i, successor.getId())) {
            return successor;
         }
         ChordMessageInterface j = closestPrecedingNode(key);

         if (j == null) {
            return null;
         }
         return j.locateSuccessor(key);
      }
      return successor;
   }

   /**
    * finds the closest node before
    *
    * @param key the key to find closest one before
    * @return the successor
    * @throws RemoteException
    */
   public ChordMessageInterface closestPrecedingNode(int key) throws RemoteException {
      int count = M - 1;
      if (key == i) {
         throw new IllegalArgumentException("Key must be distinct that  " + i);
      }
      for (count = M - 1; count >= 0; count--) {
         if (finger[count] != null && isKeyInSemiCloseInterval(finger[count].getId(), i, key)) {
            return finger[count];
         }
      }
      return successor;

   }

   /**
    * Has the key join the current ring
    *
    * @param ip the ip of joining node
    * @param port the port of the joining node
    * @throws RemoteException
    */
   public void joinRing(String ip, int port) throws RemoteException {
      try {
         System.out.println("Get Registry to joining ring");
         Registry registry = LocateRegistry.getRegistry(ip, port);
         ChordMessageInterface chord = (ChordMessageInterface) (registry.lookup("Chord"));
         predecessor = null;
         System.out.println("Locating successor to joining ring " + port);

         successor = chord.locateSuccessor(i);
         if (successor != null) {
            System.out.println("Successor " + successor.getId());
         }

      }
      catch (RemoteException e) {
         e.printStackTrace();
      }
      catch (NotBoundException e) {
         e.printStackTrace();
      }
   }

   /**
    * stabilizes the ring
    */
   public void stabilize() {
      try {
         ChordMessageInterface x = successor.getPredecessor();

         if (x != null && x.getId() != i
          && (isKeyInOpenInterval(x.getId(), i, successor.getId()) || i == successor.getId())) {
            successor = x;
         }
         if (successor.getId() != getId()) {
            successor.notify(this);
         }
      }
      catch (RemoteException e) {
         e.printStackTrace();
      }
      catch (NullPointerException e) {
         e.printStackTrace();
      }
   }

   /**
    * lets the ring know when things happen. Place to check where keys are
    * transfered
    *
    * @param j
    * @throws RemoteException
    */
   public void notify(ChordMessageInterface j) throws RemoteException {
      if (predecessor == null || (predecessor != null && isKeyInOpenInterval(j.getId(), predecessor.getId(), i))) // transfer keys in the range [j,i) to j;
      {
         predecessor = j;
//         System.out.println(this.getId());
//         System.out.println(this.successor.getId());
         this.successor.transferKeys(this);
      }
   }

   /**
    * Reads the files in folder and see if the key needs to be transfered or not
    *
    * @param j node
    * @throws RemoteException
    */
   public void transferKeys(ChordMessageInterface j) throws RemoteException {
      File folder = new File("" + getId());
      File[] listOfFiles = folder.listFiles();

      for (File file : listOfFiles) {
         if (file.isFile()) {
            System.out.println(file.getName());
            int x = Integer.parseInt(file.getName());
            System.out.println("X is within bounds " + x + "(" + j.getId() + ", " + this.getId() + ")");
            if (!(isKeyInOpenInterval(x, j.getId(), this.getId()))) {
               System.out.println("X is within bounds " + x);
               try {
                  j.put(x, this.get(x));
               }
               catch (IOException ex) {
                  Logger.getLogger(Chord.class.getName()).log(Level.SEVERE, null, ex);
               }
            }
         }
      }
   }

   /**
    * This function has the node that is leaving pass on its keys
    *
    * @throws RemoteException
    */
   public void leave() throws RemoteException {
      File folder = new File("" + getId());
      File[] listOfFiles = folder.listFiles();

      for (File file : listOfFiles) {
         if (file.isFile()) {
            System.out.println(file.getName());
            int x = Integer.parseInt(file.getName());
            try {
               successor.put(x, get(x));
            }
            catch (IOException ex) {
               Logger.getLogger(Chord.class.getName()).log(Level.SEVERE, null, ex);
            }
         }
      }
   }

   /**
    * Function to change successors
    *
    * @param j the to be successor
    * @throws RemoteException
    */
   public void setSuccessor(ChordMessageInterface j) throws RemoteException {
      this.successor = j;
   }

   /**
    * Function to change the predecessor
    *
    * @param j the to be predecessor
    * @throws RemoteException
    */
   public void setPredecessor(ChordMessageInterface j) throws RemoteException {
      this.predecessor = j;
   }

   /**
    * This function has the node leave the ring and reconnects the other nodes
    * making a complete ring
    *
    * @throws RemoteException
    */
   public void leaveRing() throws RemoteException {
      leave();
      timer.cancel();

      try {
         successor.setPredecessor(predecessor);
         predecessor.setSuccessor(successor);
      }
      catch (RemoteException e) {
         Logger.getLogger(Chord.class.getName()).log(Level.SEVERE, null, e);
      }
      successor = null;
      predecessor = null;
      System.out.println("LEAVE");
   }

   /**
    * fixes fingers
    */
   public void fixFingers() {
      if (finger[nextFinger] != null) {
         nextFinger = nextFinger + 1;
      }
      if (nextFinger >= M) {
         nextFinger = 0;
      }
      try {
         finger[nextFinger] = locateSuccessor((i + (1 << nextFinger)));
      }
      catch (RemoteException e) {
         e.printStackTrace();
      }
   }

   /**
    *
    */
   public void checkPredecessor() {
      try {
         if (predecessor != null) {
            if (!predecessor.isAlive()) {
               predecessor = null;
            }
         }
      }
      catch (RemoteException e) {
         predecessor = null;
      }
   }

   /**
    * the constructor to create a chord object
    *
    * @param port the port associated with the chord
    * @throws RemoteException
    */
   public Chord(final int port) throws RemoteException {
      int j;
      finger = new ChordMessageInterface[M];
      for (j = 0; j < M; j++) {
         finger[j] = null;
      }
      i = port;

      predecessor = null;
      successor = this;
      timer.scheduleAtFixedRate(new TimerTask() {
         @Override
         public void run() {
            stabilize();
            fixFingers();
            checkPredecessor();
         }
      }, 500, 500);
      try {
         // create the registry and bind the name and object.
         System.out.println("Starting RMI at port=" + port);
         registry = LocateRegistry.createRegistry(port);
         registry.rebind("Chord", this);
      }
      catch (RemoteException e) {
         throw e;
      }
   }

   /**
    * prints out the information of the node
    */
   void Print() {
      try {
         if (successor != null) {
            System.out.println("successor " + successor.getId());
         }
         if (predecessor != null) {
            System.out.println("predecessor " + predecessor.getId());
         }
      }
      catch (RemoteException e) {
         System.out.println("Cannot retrive id");
      }
   }
}
