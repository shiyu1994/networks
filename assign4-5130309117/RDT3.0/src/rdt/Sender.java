package rdt;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
	protected int state = 0;
	private static int blockSize = 50000;
	private static int CRC_GEN = 0x00011021;
	private JButton btnExit;
	private JLabel lblNewLabel;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				
				File path = new File("send" + File.separator);
				if(!path.exists()) 
					path.mkdir();
				
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
		
		final JButton btnSend = new JButton("Send");
		btnSend.setBounds(171, 167, 117, 29);
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lblNewLabel.setText("Start sending at: " + System.currentTimeMillis() + "ms");
				String fileName = txtEnterFileName.getText();
				File file = new File("send" + File.separator + fileName);
				if(file != null && file.exists() && file.isFile()) {
					try {
						BufferedInputStream fromFile = new BufferedInputStream(new FileInputStream(file));
						
						byte[] name = fileName.getBytes();
						byte[] fileInfo = new byte[name.length + 3];
						for(int i = 0; i < name.length; ++i)
							fileInfo[i] = name[i];
						addCRC(fileInfo, name.length);
						addSeqNum(fileInfo, name.length);
						Thread sendInfo = new Thread(new SenderCycle(fileInfo, state));
						sendInfo.start();
						sendInfo.join();
						
						state = (state + 2) % 4;
						
						byte[] length = ((Long)file.length()).toString().getBytes();
						fileInfo = new byte[length.length + 3];
						for(int i = 0; i < length.length; ++i)
							fileInfo[i] = length[i];
						addCRC(fileInfo, length.length);
						addSeqNum(fileInfo, length.length);
						sendInfo = new Thread(new SenderCycle(fileInfo, state));
						sendInfo.start();
						sendInfo.join();
						
						state = (state + 2) % 4;
						Long remains = file.length();
						int len;
						byte[] sendData;
						if(remains > blockSize - 3) {
							sendData = new byte[blockSize];
							len = blockSize - 3;
							remains -= blockSize - 3;
						}
						else {
							sendData = new byte[(int)(remains + 3)];
							len = remains.intValue();
							remains = 0L;
						}
						//TODO check the length of sendData. Always =1024?
						while(len > 0 && fromFile.read(sendData, 0, len) != -1) {
							addCRC(sendData, len);
							addSeqNum(sendData, len);
							Thread sender = new Thread(new SenderCycle(sendData, state));
							sender.start();
							sender.join();
							state = (state + 2) % 4;
							
							if(remains > blockSize - 3) {
								sendData = new byte[blockSize];
								len = blockSize - 3;
								remains -= blockSize - 3;
							}
							else {
								sendData = new byte[(int)(remains + 3)];
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
		
		lblNewLabel = new JLabel("");
		lblNewLabel.setBounds(6, 130, 393, 16);
		frame.getContentPane().add(lblNewLabel);
	}
	
	private void addCRC(byte[] data, int len) {
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
		data[len + 1] = (byte)(div & 0x0000000f);
		data[len + 2] = (byte)(div & 0x000000f0 >> 8);
	}
	
	private void addSeqNum(byte[] data, int len) {
		data[len] = (byte)(state / 2);
	}
	
	private class SenderCycle implements Runnable {

		private byte[] sendData = null;
		private int state, end;
		private DatagramSocket senderSocket;
		
		SenderCycle(byte[] sendData, int state) {
			this.sendData = sendData;
			this.state = state;
			end = (state / 2 == 0 ? 2 : 0);
			try {
				senderSocket = new DatagramSocket(2376);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			while(state != end) {
				Counter counter = new Counter(5000, this);
				switch(state % 2) {
				case 0:
					sendPacket();
					state = (state + 1) % 4;
					counter.start();
				case 1:
					try {
						byte[] ackData = new byte[2];
						DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
						senderSocket.receive(ackPacket);
						byte parity = 0x00;
						for(int j = 0; j < 8; ++j)
							parity ^= ((ackData[0] >> j) & 0x01);
						if(parity == ackData[1] && 
								((ackData[0] & 0x01) == 0x01 
								&& (((ackData[0] & 0x02) ^ (state / 2 << 1)) == 0))) {
							counter.interrupt();
							state = (state + 1) % 4;
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
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
	
	private class Counter extends Thread {
		private int time;
		private SenderCycle reSend;
		Counter(int time, SenderCycle reSend) {
			this.time = time;
			this.reSend = reSend;
		}
		public void run() {
			try {
				while(true) {
					sleep(time);
					reSend.sendPacket();
				}
			} catch (InterruptedException e) {
				//e.printStackTrace();
			}
		}
	}
}
