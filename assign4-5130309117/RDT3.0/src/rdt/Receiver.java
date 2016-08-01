package rdt;

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
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;

public class Receiver {

	private JFrame frame;
	private JTextField textField;
	private Boolean started = false;
	private static int CRC_GEN = 0x00011021;
	private JLabel label;

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
				// TODO Auto-generated method stub
				if(!started) {
					Thread receiver = new ReceiverCycle();
					receiver.start();
					started = true;
					btnStart.setText("Exit");
				}
				else {
					System.exit(0);
				}
			}
			
		});
		frame.getContentPane().add(btnStart);
		
		label = new JLabel("");
		label.setBounds(6, 144, 415, 16);
		frame.getContentPane().add(label);
	}
	
	private class ReceiverCycle extends Thread {
		
		private int state = 0;
		private byte[] ack = null;
		private int senderPort = 0;
		private InetAddress senderAddress;
		private int receiveInfo = 0;
		private Long fileLength;
		private String fileName = "";
		private BufferedOutputStream toFile;
		private static final int blockSize = 50000;
		
		public void run() {
			while(true) {
				try {
					DatagramSocket receiverSocket = new DatagramSocket(Integer.parseInt(textField.getText()));
					byte[] receiveData = new byte[blockSize];
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					receiverSocket.receive(receivePacket);
					int len = receivePacket.getLength();
					receiverSocket.close();
					senderAddress = receivePacket.getAddress();
					senderPort = receivePacket.getPort();
					System.out.println("len: " + len);
				
					if(!(checkCRC(receiveData, len - 3) && receiveData[len - 3] == (byte)state)) {
						if(ack != null) {
							System.out.println("corrupt");
							send(ack);
						}
					}
					else {
						if(receiveInfo == 0) {
							System.out.println("0");
							fileName = new String(receiveData, 0, len - 3);
							System.out.println(fileName);
							receiveInfo = 1;
						}
						else if(receiveInfo == 1) {
							System.out.println("1");
							fileLength = Long.parseLong(new String(receiveData, 0, len - 3));
							System.out.println(fileName);
							receiveInfo = 2;
							toFile = new BufferedOutputStream(new FileOutputStream("receive" + File.separator + fileName));
						}
						else if(receiveInfo == 2) {
							System.out.println("2");
							toFile.write(receiveData, 0, (int) (len - 3 > fileLength ? fileLength : len - 3));
							fileLength -= len - 3;
							System.out.println("remains: " + fileLength);
							if(fileLength == 0) {
								receiveInfo = 0;
								toFile.close();
								label.setText("Receive all at: " + System.currentTimeMillis() + "ms");
							}
						}
						ack = new byte[2];
						ack[0] = (byte) (0x01 | (state << 1));
						ack[1] = (byte) (0x01 ^ state);
						send(ack);
						state = 1 - state;
					}
				} catch (NumberFormatException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		private Boolean checkCRC(byte[] data, int len) {
			int div = 0;
			for(int i = 0; i < (2 < len ? 2 : len); ++i) {
				div = (div << (8 * i)) | data[i];
			}
			if(len > 2) {
				div = (div << 1) | ((data[2] >> 7) & 0x01);
			}
			for(int i = 0; i < 8 * len - 17; ++i) {
				if((div & 0x00010000) != 0) {
					div = ((div ^ CRC_GEN) << 1) | ((data[(i + 17) / 8] >> (7 - (i + 17) % 8)) & 0x01);
				}
				else {
					div = (div << 1) | ((data[(i + 17) / 8] >> (7 - (i + 17) % 8)) & 0x01);
				}
			}
			if((div & 0x00010000) != 0)  
				div = (div ^ CRC_GEN);
			return (data[len + 1] == (byte)(div & 0x0000000f) && data[len + 2] == (byte)(div & 0x000000f0 >> 8));
		}
		
		private void send(byte[] data) {
			System.out.println(senderAddress);
			System.out.println(senderPort);
			DatagramPacket toSender = new DatagramPacket(data, data.length, senderAddress, senderPort);
			try {
				DatagramSocket receiverSocket = new DatagramSocket(Integer.parseInt(textField.getText())); 
				receiverSocket.send(toSender);
				receiverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
	}
}
