import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;

public class Sender {

	private JFrame frame;
	private JTextField txtEnterReceiverIp;
	private JTextField txtEnterReceiverPort;
	private JTextField txtEnterFileName;
	private static int blockSize = 50000;
	private JButton btnExit;
	private JLabel label;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Sender window = new Sender();
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
	public Sender() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
		File path = new File("send" + File.separator);
		if(!path.exists()) 
			path.mkdir();
		
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		txtEnterReceiverIp = new JTextField();
		txtEnterReceiverIp.setText("Enter Receiver IP");
		txtEnterReceiverIp.setBounds(64, 46, 134, 28);
		frame.getContentPane().add(txtEnterReceiverIp);
		txtEnterReceiverIp.setColumns(10);
		
		txtEnterReceiverPort = new JTextField();
		txtEnterReceiverPort.setText("Enter Receiver Port");
		txtEnterReceiverPort.setBounds(226, 46, 134, 28);
		frame.getContentPane().add(txtEnterReceiverPort);
		txtEnterReceiverPort.setColumns(10);
		
		txtEnterFileName = new JTextField();
		txtEnterFileName.setText("Enter File Name");
		txtEnterFileName.setBounds(160, 86, 134, 28);
		frame.getContentPane().add(txtEnterFileName);
		txtEnterFileName.setColumns(10);
		
		JButton btnSend = new JButton("Send");
		btnSend.setBounds(171, 167, 117, 29);
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				label.setText("Start sending at: " + System.currentTimeMillis() + "ms");
				String fileName = txtEnterFileName.getText();
				File file = new File("send" + File.separator + fileName);
				if(file != null && file.exists() && file.isFile()) {
					try {
						BufferedInputStream fromFile = new BufferedInputStream(new FileInputStream(file));
						
						byte[] name = fileName.getBytes();
						Thread sendInfo = new Thread(new SenderCycle(name));
						sendInfo.start();
						sendInfo.join();
						
						byte[] length = ((Long)file.length()).toString().getBytes();
						sendInfo = new Thread(new SenderCycle(length));
						sendInfo.start();
						sendInfo.join();
						
						Long remains = file.length();
						int len;
						byte[] sendData;
						if(remains > blockSize) {
							sendData = new byte[blockSize];
							len = blockSize;
							remains -= blockSize;
						}
						else {
							sendData = new byte[(int)(remains + 0)];
							len = remains.intValue();
							remains = 0L;
						}
						while(len > 0 && fromFile.read(sendData, 0, len) != -1) {
							Thread sender = new Thread(new SenderCycle(sendData));
							sender.start();
							sender.join();
							
							if(remains > blockSize) {
								sendData = new byte[blockSize];
								len = blockSize;
								remains -= blockSize;
							}
							else {
								sendData = new byte[(int)(remains + 0)];
								len = remains.intValue();
								remains = 0L;
							}
						}
						fromFile.close();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		frame.getContentPane().add(btnSend);
		
		btnExit = new JButton("Exit");
		btnExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		btnExit.setBounds(171, 208, 117, 29);
		frame.getContentPane().add(btnExit);
		
		label = new JLabel("");
		label.setBounds(6, 139, 393, 16);
		frame.getContentPane().add(label);
	}
	
	private class SenderCycle implements Runnable {

		private byte[] sendData = null;
		private DatagramSocket senderSocket;
		
		SenderCycle(byte[] sendData) {
			this.sendData = sendData;
			try {
				senderSocket = new DatagramSocket(2376);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			sendPacket();
			senderSocket.close();
		}
		
		private void sendPacket() {
			int port = Integer.parseInt(txtEnterReceiverPort.getText());
			try {
				InetAddress IPAddress = InetAddress.getByName(txtEnterReceiverIp.getText());
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				senderSocket.send(sendPacket);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
