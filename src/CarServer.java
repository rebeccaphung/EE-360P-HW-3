import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CarServer {

  public static HashMap<Car, AtomicInteger> cars = new HashMap<Car, AtomicInteger>();

  public static void main (String[] args) {
    int tcpPort;
    int udpPort;
    if (args.length != 1) {
      System.out.println("ERROR: Provide 1 argument: input file containing initial inventory");
      System.exit(-1);
    }
    String fileName = args[0];
    tcpPort = 7000;
    udpPort = 8000;
    int len = 1024;

    // parse the inventory file

    Scanner sc = null;
    try {
      sc = new Scanner(new FileReader(fileName));

      while(sc.hasNextLine()) {
        String cmd = sc.nextLine();
        String[] tokens = cmd.split(" ");

        Car newCar = new Car(tokens[0], tokens[1]);
        int quantity = Integer.parseInt(tokens[2]);
        AtomicInteger atomicQuantity = new AtomicInteger(quantity);

        cars.put(newCar, atomicQuantity);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }



    // TODO: handle request from clients


  }

  public class UDPServerThread extends Thread{

    int port, len;

    public UDPServerThread(int port, int len){
      this.port = port;
      this.len = len;
    }
    @Override
    public void run() {
      DatagramPacket datapacket, returnpacket;

      try {
        DatagramSocket datasocket = new DatagramSocket(port);
        byte[] buf = new byte[len];
        while (true) {
          datapacket = new DatagramPacket(buf, buf.length);
          datasocket.receive(datapacket);
          returnpacket = new DatagramPacket(
                  datapacket.getData(),
                  datapacket.getLength(),
                  datapacket.getAddress(),
                  datapacket.getPort());
          datasocket.send(returnpacket);
        }
      } catch (SocketException e) {
        System.err.println(e);
      } catch (IOException e) {
        System.err.println(e);
      }
    }
  }

  public class TCPServerThread extends Thread{
    Socket theClient;
    public TCPServerThread(Socket s) {
      theClient = s;
    }
    public void run() {
      try {
        Scanner sc = new Scanner(theClient.getInputStream());
        PrintWriter pout = new PrintWriter(theClient.getOutputStream());
        String command = sc.nextLine();
        System.out.println("received:" + command);
        Scanner st = new Scanner(command);
        String tag = st.next();
        if (tag.equals("search")) {
          InetSocketAddress addr = table.search(st.next());
          if (addr == null) pout.println(0 + " " + "nullhost");
          else pout.println(addr.getPort() + " " + addr.getHostName());
        } else if (tag.equals("insert")) {
          String name = st.next();
          String hostName = st.next();
          int port = st.nextInt();
          int retValue = table.insert(name, hostName, port);
          pout.println(retValue);
        } else if (tag.equals("blockingFind")) {
          InetSocketAddress addr = table.blockingFind(st.next());
          pout.println(addr.getPort() + " " + addr.getHostName());
        } else if (tag.equals("clear")) {
          table.clear();
        }
        pout.flush();
        theClient.close();
      } catch (IOException e) {
        System.err.println(e);
      }

    }
  }
}
