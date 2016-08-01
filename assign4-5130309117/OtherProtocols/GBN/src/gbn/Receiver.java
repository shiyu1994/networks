package gbn;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;

public class Receiver {

	private JFrame frame;
	private JTextField textField;
	private static int SEQ_NUMBER_BOUND = 64;
	private Boolean started = false;
	private DatagramSocket receiverSocket;
	private JLabel lblNewLabel;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				
				File path = new File("receive" + File.separator);
				if(!path.exists()) 
					path.mkdir();
				
				try {
					Receiver window = new Receiver();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Receiver() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JLabel lblIpAddress = new JLabel("IP Address:");
		try {
			lblIpAddress = new JLabel("IP Address:" + InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		lblIpAddress.setBounds(36, 46, 352, 16);
		frame.getContentPane().add(lblIpAddress);
		
		JLabel lblPortNumber = new JLabel("Port Number:");
		lblPortNumber.setBounds(36, 75, 120, 16);
		frame.getContentPane().add(lblPortNumber);
		
		textField = new JTextField();
		textField.setBounds(124, 69, 134, 28);
		frame.getContentPane().add(textField);
		textField.setColumns(10);
		
		final JButton btnStart = new JButton("Start");
		btnStart.setBounds(171, 167, 117, 29);
		btnStart.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(!started) {
					ReceiverThread receiver = new ReceiverThread();
					receiver.start();
					btnStart.setText("Exit");
					started = true;
				}
				else {
					receiverSocket.close();
					System.exit(0);
				}
			}
			
		});
		frame.getContentPane().add(btnStart);
		
		lblNewLabel = new JLabel("");
		lblNewLabel.setBounds(6, 143, 418, 16);
		frame.getContentPane().add(lblNewLabel);
	}
	
	private class ReceiverThread extends Thread {
		
		private int senderPort = 0;
		private InetAddress senderAddress;
		private Long fileLength;
		private String fileName = "";
		private BufferedOutputStream toFile;
		private static final int blockSize = 20480;
		private static final int headerSize = 8;
		private int nextToReceive = -2;
		private DatagramPacket ACK = null; 
		
		public void run() {
			try {
				receiverSocket = new DatagramSocket(Integer.parseInt(textField.getText()));
			} catch (NumberFormatException | SocketException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			while(true) {
				try {
					byte[] receiveData = new byte[blockSize + headerSize];
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					receiverSocket.receive(receivePacket);
					int len = receivePacket.getLength();
					senderAddress = receivePacket.getAddress();
					senderPort = receivePacket.getPort();
					//System.out.println("len: " + len);
					//System.out.println(extractSeqNumber(receiveData)); 
					if(nextToReceive == -2) {
						fileName = new String(receiveData, 0, len);
						fileName = "receive" + File.separator + fileName;
						toFile = new BufferedOutputStream(new FileOutputStream(new File(fileName)));
						++nextToReceive;
					}
					else if(nextToReceive == -1) {
						fileLength = Long.parseLong(new String(receiveData, 0, len));
						++nextToReceive;
					}
					else if(!corrupted(receiveData) && extractSeqNumber(receiveData) == nextToReceive) {
						
							//System.out.println("in receiver " + fileLength);
							toFile.write(receiveData, headerSize, (int)(fileLength < len - headerSize ? fileLength : len - headerSize));
							toFile.flush();
							byte[] ACKBytes = new byte[8];
							for(int i = 0; i < 4; ++i) {
								ACKBytes[i] = (byte)((nextToReceive >> (i * 8)) & 0x000000ff);
							}
							addCRC(ACKBytes);
							ACK = new DatagramPacket(ACKBytes, 0, 8, senderAddress, senderPort);
							receiverSocket.send(ACK);
							fileLength -= (len - headerSize);
							if(fileLength == 0) {
								//System.out.println("get all");
								toFile.close();
								nextToReceive = -2;
								lblNewLabel.setText("Receive all at " + System.currentTimeMillis() + "ms");
							}
							else 
								nextToReceive = (nextToReceive + 1) % SEQ_NUMBER_BOUND;  
					}
					else {
						if(ACK != null) {
							receiverSocket.send(ACK);
						}
						else {
							byte[] ACKBytes = new byte[8];
							for(int i = 0; i < 4; ++i) {
								ACKBytes[i] = (byte)((-1 >> i) & 0x000000ff);
							}
							addCRC(ACKBytes);
							ACK = new DatagramPacket(ACKBytes, 0, 8, senderAddress, senderPort);
							receiverSocket.send(ACK);
						}
					}  
				} catch (NumberFormatException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		private void addCRC(byte[] ACK) {
			//TODO
		}
		
		private Boolean corrupted(byte[] ACK) {
			//TODO
			return false;
		}
		
		private int extractSeqNumber(byte[] data) {
			int seqNumber = 0;
			for(int i = 0; i < 4; ++i) {
				//System.out.println("print " + i + " :" + data[i]);
				seqNumber |= (data[i] << (i * 8));
			}
			return seqNumber;
		}
	}
}
