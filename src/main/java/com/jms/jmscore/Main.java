package com.jms.jmscore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Main {
	private static final Logger logger = Logger.getLogger(Main.class.getName());
	private static MessageConsumer consumer;
	private static Session session;
	private static Connection connection;
	private static ActiveMQConnectionFactory factory;
	private static java.sql.Connection connectionDb;
	private static Boolean running = true;

	public static void main(String[] args) throws Exception {
		logger.setLevel(Level.ALL);
		logger.info("Start JMS Service...");

		Boolean checkMessage = false;
		Boolean schedule = false;
		if (args.length > 0 && (!args[0].isEmpty() || !args[1].isEmpty())) {
			if (args[0].equals("true")) {
				checkMessage = true;
			}
			if (args.length > 1 && args[1].equals("true")) {
				schedule = true;
			}
		}

		long startTime = System.currentTimeMillis();

		Properties properties = new Properties();
		InputStream inputStream = Main.class.getResourceAsStream("/application.properties");

		try {
			properties.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}

		factory = new ActiveMQConnectionFactory();
		factory.setBrokerURL(properties.getProperty("artemis.url"));
		factory.setUser(properties.getProperty("artemis.username"));
		factory.setPassword(properties.getProperty("artemis.password"));

		connection = factory.createConnection();
		connection.start();

		session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		Queue queue = session.createQueue(properties.getProperty("artemis.queue-log"));
		consumer = session.createConsumer(queue);

		connectionDb = DriverManager.getConnection(properties.getProperty("datasource.url"),
				properties.getProperty("datasource.username"), properties.getProperty("datasource.password"));

		if ((schedule && checkMessage) || (!checkMessage && !schedule)) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						running = false;
						if (consumer != null) {
							consumer.close();
						}
						if (session != null) {
							session.close();
						}
						if (connection != null) {
							connection.close();
						}
						if (factory != null) {
							factory.close();
						}
						if (connectionDb != null) {
							connectionDb.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						System.exit(0);
					}
				}
			});
			if (checkMessage) {
				try {
					while (running) {
						checkMessage(properties);
						Thread.sleep(Long.parseLong(properties.getProperty("time-schedule-check")));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				getMsg(consumer, session, connection, factory, connectionDb, properties);
			}
		} else {
			checkMessage(properties);
		}

		consumer.close();
		session.close();
		connection.close();
		factory.close();
		connectionDb.close();

		System.exit(0);

		logger.info("Time To Done Operation " + (System.currentTimeMillis() - startTime) / 1000.0 + " Second");
	}

	private static void checkMessage(Properties properties) throws JMSException, Exception {
		logger.info("Start Check Message To Queue");
		Set<String> datasOut = new HashSet<>();
		Set<String> datasIn = new HashSet<>();
		Enumeration<?> messagesOut = session
				.createBrowser(session.createQueue(properties.getProperty("artemis.queue-out")))
				.getEnumeration();
		while (messagesOut.hasMoreElements()) {
			Message message = (Message) messagesOut.nextElement();
			if (message instanceof TextMessage) {
				datasOut.add(message.getObjectProperty("JmsId").toString());
			}
		}

		Enumeration<?> messagesIn = session
				.createBrowser(session.createQueue(properties.getProperty("artemis.queue-in")))
				.getEnumeration();
		while (messagesIn.hasMoreElements()) {
			Message message = (Message) messagesIn.nextElement();
			if (message instanceof TextMessage) {
				datasIn.add(message.getObjectProperty("JmsId").toString());
			}
		}

		int batchSize = Integer.parseInt(properties.getProperty("batch-size-update"));
		Set<Set<String>> chunkDatasIn = chunkSet(datasIn, batchSize);
		Set<Set<String>> chunkDataOut = chunkSet(datasOut, batchSize);

		for (Set<String> dataIn : chunkDatasIn) {
			updateDataBatch(dataIn, connectionDb, properties, false);
		}

		for (Set<String> dataOut : chunkDataOut) {
			updateDataBatch(dataOut, connectionDb, properties, true);
		}

		if (chunkDatasIn.size() == 0) {
			updateDataBatch(null, connectionDb, properties, false);
		}
		if (chunkDataOut.size() == 0) {
			updateDataBatch(null, connectionDb, properties, true);
		}

		logger.info("Done Check Message To Queue");
	}

	private static void getMsg(MessageConsumer consumer, Session session, Connection connection,
			ActiveMQConnectionFactory factory, java.sql.Connection connectionDb,
			Properties properties) throws Exception {

		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
		Set<TextMessage> textList = new HashSet<>();

		try {
			Set<LogModel> datasIn = new HashSet<>();
			Set<LogModel> datasOut = new HashSet<>();
			int batchSize = Integer.parseInt(properties.getProperty("batch-size-insert"));
			consumer.setMessageListener(new MessageListener() {
				int count = 0;

				public void onMessage(Message msg) {
					if (msg instanceof TextMessage) {
						try {
							TextMessage message = (TextMessage) msg;
							Object jmsId = message.getObjectProperty(properties.getProperty("JmsId"));
							Object date = message.getObjectProperty(properties.getProperty("Date"));
							Object amdCid = message.getObjectProperty(properties.getProperty("AMQ_CID"));
							Object amqOrigQueue = message.getObjectProperty(properties.getProperty("AMQ_ORIG_QUEUE"));
							Object amqOrigAddress = message
									.getObjectProperty(properties.getProperty("AMQ_ORIG_ADDRESS"));
							Object amqOrigMessageId = message
									.getObjectProperty(properties.getProperty("AMQ_ORIG_MESSAGE_ID"));
							Object hdrBrokerInTime = message
									.getObjectProperty(properties.getProperty("HDR_BROKER_IN_TIME"));
							Object hdrMessageId = message.getObjectProperty(properties.getProperty("HDR_MESSAGE_ID"));
							Object eksReference = getFieldFromXml("EksReference", message.getText());
							String eksRef = null;
							String reasonError = null;

							logger.info("Message Received With Jms ID : " + jmsId);

							if (eksReference instanceof String) {
								eksRef = eksReference.toString();
							} else if (eksReference instanceof Exception) {
								reasonError = eksReference.toString();
							}

							LogModel model = new LogModel(
									(jmsId != null ? jmsId.toString() : null),
									(date != null ? new Date(sdf.parse(date.toString()).getTime())
											: null),
									message.getText(),
									eksRef,
									(amdCid != null ? amdCid.toString() : null),
									(amqOrigQueue != null ? amqOrigQueue.toString() : null),
									(amqOrigAddress != null ? amqOrigAddress.toString() : null),
									(amqOrigMessageId != null ? Long.parseLong(amqOrigMessageId.toString()) : null),
									(hdrBrokerInTime != null ? Long.parseLong(hdrBrokerInTime.toString()) : null),
									(hdrMessageId != null ? hdrMessageId.toString() : null),
									reasonError,
									new Date());

							if (model.getAmqOrigAddress()
									.equalsIgnoreCase(properties.getProperty("artemis.queue-in"))) {
								datasIn.add(model);
								textList.add(message);
							} else if (model.getAmqOrigAddress()
									.equalsIgnoreCase(properties.getProperty("artemis.queue-out"))) {
								datasOut.add(model);
								textList.add(message);
							}

							count++;
							if (count % batchSize == 0) {
								logger.info("Batch " + count + " mencapai " + batchSize + " pesan.");
								boolean insertSuccessful = false;
								try {
									Long start = System.currentTimeMillis();
									logger.info("Start Insert To Database...");
									insertDataBatch(datasOut, datasIn, connectionDb);
									logger.info("Done Insert To Database With Time : "
											+ ((System.currentTimeMillis() - start) / 1000.00) + " Seconds");
									insertSuccessful = true;
								} catch (Exception e) {
									logger.info("Error during insertDataBatch: " + e.getMessage());
									e.printStackTrace();
									throw e;
								}
								if (insertSuccessful) {
									logger.info("Start Acknowledge Message...");
									for (TextMessage textMessage : textList) {
										textMessage.acknowledge();
									}
									logger.info("Done Acknowledge Message...");
								} else {
									logger.info("Acknowledge skipped due to insert failure.");
								}
								datasOut.clear();
								datasIn.clear();
								textList.clear();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});

			try {
				while (true) {
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			logger.info("Error Get");
			e.printStackTrace();
			throw e;
		}
	}

	private static void insertDataBatch(Set<LogModel> datasOut, Set<LogModel> datasIn, java.sql.Connection connection)
			throws Exception {
		Properties properties = new Properties();
		InputStream inputStream = Main.class.getResourceAsStream("/application.properties");
		try {

			try {
				properties.load(inputStream);
			} catch (IOException e) {
				e.printStackTrace();
			}

			DatabaseService databaseService = new DatabaseServiceImpl(connection);
			StringBuilder queryOut = new StringBuilder(
					"INSERT INTO " + properties.getProperty("datasource.schema.out"));
			StringBuilder queryIn = new StringBuilder(
					"INSERT INTO " + properties.getProperty("datasource.schema.in"));
			String query = " (JMS_ID, DATE_TIME, MESSAGE, EKS_REFERENCE, AMQ_CID, AMQ_ORIG_QUEUE, AMQ_ORIG_ADDRESS, AMQ_ORIG_MESSAGE_ID, HDR_BROKER_IN_TIME, HDR_MESSAGE_ID, IS_CONSUME, REASON_ERROR, CREATED_AT) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			Set<Object[]> batchDataOut = new HashSet<>();
			for (LogModel data : datasOut) {
				Object[] params = new Object[13];
				params[0] = data.getJmsId();
				params[1] = data.getDateTime() == null ? null : new java.sql.Timestamp(data.getDateTime().getTime());
				params[2] = data.getMessage();
				params[3] = data.getEksReference();
				params[4] = data.getAmqCid();
				params[5] = data.getAmqOrigQueue();
				params[6] = data.getAmqOrigAddress();
				params[7] = data.getAmqOrigMessageId();
				params[8] = data.getHdrBrokerInTime();
				params[9] = data.getHdrMessageId();
				params[10] = 1L;
				params[11] = data.getReasonError();
				params[12] = new java.sql.Date(data.getCreatedAt().getTime());
				batchDataOut.add(params);
			}

			Set<Object[]> batchDataIn = new HashSet<>();
			for (LogModel data : datasIn) {
				Object[] params = new Object[13];
				params[0] = data.getJmsId();
				params[1] = data.getDateTime() == null ? null : new java.sql.Timestamp(data.getDateTime().getTime());
				params[2] = data.getMessage();
				params[3] = data.getEksReference();
				params[4] = data.getAmqCid();
				params[5] = data.getAmqOrigQueue();
				params[6] = data.getAmqOrigAddress();
				params[7] = data.getAmqOrigMessageId();
				params[8] = data.getHdrBrokerInTime();
				params[9] = data.getHdrMessageId();
				params[10] = 1L;
				params[11] = data.getReasonError();
				params[12] = new java.sql.Date(data.getCreatedAt().getTime());
				batchDataIn.add(params);
			}

			queryOut.append(query);
			queryIn.append(query);

			int countOut = 0;
			int countIn = 0;

			if (!batchDataOut.isEmpty()) {
				countOut = databaseService.queryInsert(queryOut.toString(), batchDataOut);
			}
			if (!batchDataIn.isEmpty()) {
				countIn = databaseService.queryInsert(queryIn.toString(), batchDataIn);
			}

			logger.info("Row Affected Log IN : " + countIn);
			logger.info("Row Affected Log OUT : " + countOut);
		} catch (SQLException e) {
			logger.info("Error Insert");
			e.printStackTrace();
			throw e;
		}
	}

	private static int updateDataBatch(Set<String> datas, java.sql.Connection connection, Properties properties,
			Boolean isOut) throws Exception {
		try {
			DatabaseService databaseService = new DatabaseServiceImpl(connection);
			StringBuilder query = new StringBuilder("UPDATE " + properties.getProperty("datasource.schema.in"));
			if (isOut) {
				query = new StringBuilder("UPDATE " + properties.getProperty("datasource.schema.out"));
			}
			if (datas != null) {
				StringBuilder queryIn = new StringBuilder("(");
				for (int i = 0; i < datas.size(); i++) {
					queryIn.append("?");
					if (i < datas.size() - 1) {
						queryIn.append(", ");
					}
				}
				queryIn.append(") ");

				query.append(" SET IS_CONSUME = CASE " +
						"WHEN JMS_ID IN " + queryIn.toString() + "THEN 0 " +
						"WHEN JMS_ID NOT IN " + queryIn.toString() + "AND IS_CONSUME = 0 THEN 1 " +
						"ELSE IS_CONSUME " +
						"END " +
						"WHERE JMS_ID IN " + queryIn.toString() +
						"OR (JMS_ID NOT IN " + queryIn.toString() + "AND IS_CONSUME = 0)");
			} else {
				query.append(" SET IS_CONSUME = 1 " +
						"WHERE IS_CONSUME = 0");
			}

			return databaseService.queryUpdate(query.toString(), datas);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public static Set<Set<String>> chunkSet(Set<String> set, int chunkSize) {
		Set<Set<String>> chunks = new HashSet<>();
		List<String> list = new ArrayList<>(set);

		for (int i = 0; i < list.size(); i += chunkSize) {
			Set<String> chunk = new HashSet<>(list.subList(i, Math.min(i + chunkSize, list.size())));
			chunks.add(chunk);
		}

		return chunks;
	}

	private static Object getFieldFromXml(String field, String data) {
		try {
			if (data == null) {
				throw new Exception("Message Was Null Or Empty");
			}
			String result = null;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			InputStream inputStream = new ByteArrayInputStream(data.getBytes("UTF-8"));
			Document document = builder.parse(inputStream);

			NodeList nodeList = document.getElementsByTagName("ReqMsg");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element msgElement = (Element) node;
					result = getTagValue(field, msgElement);
				}
			}

			if (result == null) {
				throw new Exception("Tag '" + field + "' not found in XML Message");
			}

			return result;
		} catch (Exception e) {
			logger.info(e.toString());
			return e;
		}
	}

	private static String getTagValue(String tag, Element element) {
		NodeList nodeList = element.getElementsByTagName(tag);
		if (nodeList.getLength() > 0) {
			Node node = nodeList.item(0);
			return node.getTextContent();
		}
		return null;
	}
}
