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
	private JLabel label;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
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
		label.setBounds(18, 139, 415, 16);
		frame.getContentPane().add(label);
	}
	
	private class ReceiverCycle extends Thread {
		private int receiveInfo = 0;
		private Long fileLength;
		private String fileName = "";
		private BufferedOutputStream toFile;
		private static final int blockSize = 50000;
		
		public void run() {
			File path = new File("receive" + File.separator);
			if(!path.exists()) 
				path.mkdir();
			while(true) {
				try {
					DatagramSocket receiverSocket = new DatagramSocket(Integer.parseInt(textField.getText()));
					byte[] receiveData = new byte[blockSize];
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					receiverSocket.receive(receivePacket);
					int len = receivePacket.getLength();
					receiverSocket.close();
					System.out.println("len: " + len);
						if(receiveInfo == 0) {
							System.out.println("0");
							fileName = new String(receiveData, 0, len);
							System.out.println(fileName);
							receiveInfo = 1;
						}
						else if(receiveInfo == 1) {
							System.out.println("1");
							fileLength = Long.parseLong(new String(receiveData, 0, len));
							System.out.println(fileName);
							receiveInfo = 2;
							toFile = new BufferedOutputStream(new FileOutputStream("receive" + File.separator + fileName));
						}
						else if(receiveInfo == 2) {
							System.out.println("2");
							if(fileLength < 0)
								fileLength = 0L;
							toFile.write(receiveData, 0, (int) (len > fileLength ? fileLength : len));
							fileLength -= len;
							System.out.println("remains: " + fileLength);
							if(fileLength <= 0) {
								receiveInfo = 0;
								toFile.close();
								label.setText("Receive all at: " + System.currentTimeMillis() + "ms");
							}
						}
				} catch (NumberFormatException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
