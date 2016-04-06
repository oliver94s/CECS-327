
import java.rmi.*;
import java.io.*;

public interface ChordMessageInterface extends Remote {

   public ChordMessageInterface getPredecessor() throws RemoteException;

   ChordMessageInterface locateSuccessor(int key) throws RemoteException;

   ChordMessageInterface closestPrecedingNode(int key) throws RemoteException;

   public void joinRing(String Ip, int port) throws RemoteException;

   public void notify(ChordMessageInterface j) throws RemoteException;

   public void transferKeys(ChordMessageInterface j) throws RemoteException;

   public void setPredecessor(ChordMessageInterface j) throws RemoteException;
   
   public void setSuccessor(ChordMessageInterface j) throws RemoteException;
   
   public void leave() throws RemoteException;

   public boolean isAlive() throws RemoteException;

   public int getId() throws RemoteException;

   public void put(int guid, byte[] data) throws IOException, RemoteException;

   public byte[] get(int id) throws IOException, RemoteException;

   public void delete(int id) throws IOException, RemoteException;
}
