
import java.rmi.*;
import java.net.*;
import java.util.*;
import java.io.*;

/**
 *
 * @author olive_000
 */
public class ChordUser {

   int port;

   /**
    * constructor to create a ChordUser
    * @param p the port number
    */
   public ChordUser(int p) {
      port = p;

      Timer timer1 = new Timer();
      timer1.scheduleAtFixedRate(new TimerTask() {
         @Override
         public void run() {
            try {

               Chord chord = new Chord(port);
               System.out.println("Usage: \n\tjoin <port>\n\twrite <file> (the file must be an integer stored in the working directory, i.e, ./port/file");
               System.out.println("\tread <file>\n\tdelete <file>\n");

               Scanner scan = new Scanner(System.in);
               String delims = "[ ]+";
               String command = "";
               while (true) {
                  String text = scan.nextLine();
                  String[] tokens = text.split(delims);
                  if (tokens[0].equals("join") && tokens.length == 2) {
                     try {
                        chord.joinRing("localhost", Integer.parseInt(tokens[1]));
                     }
                     catch (IOException e) {
                        System.out.println("Error joining the ring!");
                     }
                  }
                  if (tokens[0].equals("join") && tokens.length == 3) {
                     try {
                        chord.joinRing(tokens[1], Integer.parseInt(tokens[2]));
                     }
                     catch (IOException e) {
                        System.out.println("Error joining the ring!");
                     }
                  }
                  if (tokens[0].equals("print")) {
                     chord.Print();
                  }
                  if (tokens[0].equals("leave")) {
                     chord.leaveRing();
                  }
                  if (tokens[0].equals("write") && tokens.length == 2) {

                     try {
                        String path = ".\\" + port + "\\" + Integer.parseInt(tokens[1]); // path to file
                        File f = new File(path);
                        FileInputStream fis = null;
                        fis = new FileInputStream(f);
                        byte[] data = new byte[fis.available()];
                        fis.read(data); // read data
                        fis.close();
                        ChordMessageInterface successor = chord.locateSuccessor(Integer.parseInt(tokens[1]));
                        successor.put(Integer.parseInt(tokens[1]), data); // put file into ring
                     }
                     catch (FileNotFoundException e1) {
                        //e1.printStackTrace();
                        System.out.println("File was not found!");
                     }
                     catch (IOException e) {
                        //e.printStackTrace();
                        System.out.println("Could not put file!");
                     }
                  }
                  if (tokens[0].equals("read") && tokens.length == 2) {
                     try {
                        ChordMessageInterface successor = chord.locateSuccessor(Integer.parseInt(tokens[1]));
                        String address = new String(successor.get(Integer.parseInt(tokens[1])));
                        System.out.print(address);
                     }
                     catch (IOException e) {
                        System.out.println("Could not get file!");
                     }
                  }
                  if (tokens[0].equals("delete") && tokens.length == 2) {
                     try {
                        ChordMessageInterface successor = chord.locateSuccessor(Integer.parseInt(tokens[1]));
                        successor.delete(Integer.parseInt(tokens[1]));
                     }
                     catch (IOException e) {
                        System.out.println("Could not delete file!");
                     }
                  }
               }
            }
            catch (RemoteException e) {
            }
         }
      }, 1000, 1000);
   }

   /**
    * 
    * @param args
    */
   static public void main(String args[]) {
      if (args.length < 1) {
         throw new IllegalArgumentException("Parameter: <port>");
      }
      try {
         ChordUser chordUser = new ChordUser(Integer.parseInt(args[0]));
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }
}
