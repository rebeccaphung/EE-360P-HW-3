import java.util.Scanner;
import java.io.*;
import java.util.*;
import java.net.*;

public class CarClient {
  public static void main (String[] args) {
    String hostAddress;
    int tcpPort;
    int udpPort;
    int clientId;
    boolean isTCP = false;

    if (args.length != 2) {
      System.out.println("ERROR: Provide 2 arguments: commandFile, clientId");
      System.out.println("\t(1) <command-file>: file with commands to the server");
      System.out.println("\t(2) client id: an integer between 1..9");
      System.exit(-1);
    }

    String commandFile = args[0];
    clientId = Integer.parseInt(args[1]);
    hostAddress = "localhost";
    tcpPort = 7050;// hardcoded -- must match the server's tcp port
    udpPort = 8050;// hardcoded -- must match the server's udp port

      File clientOutFile = new File("out_" + clientId + ".txt");

      FileWriter fw = null;

      try{
          fw = new FileWriter(clientOutFile);
      } catch (Exception e){
          e.printStackTrace();
      }

      PrintWriter pw = new PrintWriter(fw, true);

    try {
      InetAddress ia = InetAddress.getByName(hostAddress);
      DatagramSocket datasocket = new DatagramSocket();
      Socket socket = new Socket(hostAddress, tcpPort);

        Scanner sc = new Scanner(new FileReader(commandFile));

        while(sc.hasNextLine()) {
          String cmd = sc.nextLine();
          String[] tokens = cmd.split(" ");

          if (tokens[0].equals("setmode")) {
            // TODO: set the mode of communication for sending commands to the server
            if(tokens[1].equals("U")){
              isTCP = false;
            }
            if(tokens[1].equals("T")){
              isTCP = true;
            }
          }
          else if (tokens[0].equals("rent")) {
            // TODO: send appropriate command to the server and display the
            if(isTCP){
                TCPRequest(cmd, socket, pw);
            }
            else{
                UDPRequest(cmd, datasocket, ia, udpPort, pw);
            }
            // appropriate responses form the server
          } else if (tokens[0].equals("return")) {
            // TODO: send appropriate command to the server and display the
            // appropriate responses form the server
              if(isTCP){
                  TCPRequest(cmd, socket, pw);
              }
              else{
                  UDPRequest(cmd, datasocket, ia, udpPort, pw);
              }
          } else if (tokens[0].equals("inventory")) {
            // TODO: send appropriate command to the server and display the
            // appropriate responses form the server
              if(isTCP){
                  TCPRequest(cmd, socket, pw);
              }
              else{
                  UDPRequest(cmd, datasocket, ia, udpPort, pw);
              }
          } else if (tokens[0].equals("list")) {
            // TODO: send appropriate command to the server and display the
            // appropriate responses form the server
              if(isTCP){
                  TCPRequest(cmd, socket, pw);
              }
              else{
                  UDPRequest(cmd, datasocket, ia, udpPort, pw);
              }
          } else if (tokens[0].equals("exit")) {
            // TODO: send appropriate command to the server
                  TCPRequest(cmd, socket, pw);
                  socket.close();
                  UDPRequest(cmd, datasocket, ia, udpPort, pw);
                  datasocket.close();

          } else {
            System.out.println("ERROR: No such command");
          }
        }
    } catch (FileNotFoundException e) {
	e.printStackTrace();
    } catch (UnknownHostException e) {
      System.err.println(e);
    } catch (SocketException e) {
      System.err.println(e);
    } catch (IOException e) {
      System.err.println(e);
    }

  }

  public static void TCPRequest(String cmd, Socket socket, PrintWriter pw){
      try {
          PrintWriter reqWriter = new PrintWriter(socket.getOutputStream(), true);
          BufferedReader respReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

          reqWriter.println(cmd);

          String resp;
          if((resp = respReader.readLine()) != null){
              resp = resp.replaceAll(">","\n");
              pw.println(resp);
          }
      }
      catch (Exception e){
          e.printStackTrace();
      }

  }

  public static void UDPRequest(String cmd, DatagramSocket socket, InetAddress ia, int port, PrintWriter pw){
      try {
          byte[] buf = new byte[cmd.length()];
          byte[] resp = new byte[4096];

          buf = cmd.getBytes();

          DatagramPacket reqPacket = new DatagramPacket(buf, cmd.length(), ia, port);
          socket.send(reqPacket);

          DatagramPacket respPacket = new DatagramPacket(resp, resp.length);
          socket.receive(respPacket);

          String output = new String(respPacket.getData(), 0, respPacket.getLength());
          pw.println(output);
      }
      catch(Exception e){
          e.printStackTrace();
      }
  }

}
