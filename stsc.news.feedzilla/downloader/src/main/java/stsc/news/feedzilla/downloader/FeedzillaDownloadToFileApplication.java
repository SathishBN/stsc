package stsc.news.feedzilla.downloader;

import graef.feedzillajava.Article;
import graef.feedzillajava.Category;
import graef.feedzillajava.Subcategory;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;

import stsc.news.feedzilla.FeedzillaHashStorage;
import stsc.news.feedzilla.file.schema.FeedzillaFileArticle;
import stsc.news.feedzilla.file.schema.FeedzillaFileCategory;
import stsc.news.feedzilla.file.schema.FeedzillaFileSubcategory;

final class FeedzillaDownloadToFileApplication implements LoadFeedReceiver {

	static {
		System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "./config/log4j2.xml");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	private static Logger logger = LogManager.getLogger(FeedzillaDownloadToFileApplication.class);
	private static String DEVELOPER_FILENAME = "feedzilla_developer.properties";
	private static FeedzillaDownloadToFileApplication downloadApplication;

	private final String feedFolder;
	private boolean endlessCycle = false;
	private int daysBackDownloadFrom = 3650;
	private final FeedDataDownloader downloader;
	private final FeedzillaHashStorage hashStorage;

	FeedzillaDownloadToFileApplication() throws SQLException, IOException {
		this(DEVELOPER_FILENAME);
	}

	FeedzillaDownloadToFileApplication(String propertyFile) throws IOException {
		this.feedFolder = readFeedFolderProperty(propertyFile);
		if (feedFolder == null) {
			throw new IOException("There is no setting 'feed.folder' at property file: " + propertyFile);
		}
		this.downloader = new FeedDataDownloader(100);
		this.hashStorage = new FeedzillaHashStorage(feedFolder);
		downloader.addReceiver(this);
		if (endlessCycle) {
			daysBackDownloadFrom = 2;
		}
		hashStorage.readFeedData(DownloadHelper.createDateTimeElement(daysBackDownloadFrom), false);
	}

	private String readFeedFolderProperty(String propertyFile) throws FileNotFoundException, IOException {
		try (DataInputStream inputStream = new DataInputStream(new FileInputStream("./config/" + propertyFile))) {
			final Properties properties = new Properties();
			properties.load(inputStream);
			final String daysBackDownloadFrom = properties.getProperty("days.back.download.from");
			if (daysBackDownloadFrom != null) {
				this.daysBackDownloadFrom = Integer.valueOf(daysBackDownloadFrom);
			}
			final String endlessCycle = properties.getProperty("endless.cycle");
			if (endlessCycle != null) {
				this.endlessCycle = Boolean.valueOf(endlessCycle);
			}
			return properties.getProperty("feed.folder");
		}
	}

	void start() throws FileNotFoundException, IOException, InterruptedException {
		if (endlessCycle) {
			startEndless();
		} else {
			startNcycles();
		}
	}

	void startEndless() throws FileNotFoundException, IOException {
		LocalDateTime lastDownloadDate = LocalDateTime.now().minusDays(2).withHour(0).withMinute(0);
		while (!downloader.isStopped()) {
			final LocalDateTime now = LocalDateTime.now();
			if (downloadIteration(lastDownloadDate)) {
				lastDownloadDate = now;
			}
		}
		logger.info("Stopping now true, we break endless cycle");
	}

	void startNcycles() throws FileNotFoundException, IOException, InterruptedException {
		for (int i = daysBackDownloadFrom; i > 1; --i) {
			if (downloader.isStopped()) {
				break;
			}
			downloadIteration(DownloadHelper.createDateTimeElement(i));
		}
		downloader.stopDownload();
	}

	private boolean downloadIteration(LocalDateTime downloadFrom) throws FileNotFoundException, IOException {
		downloader.setDaysToDownload(downloadFrom);
		final boolean result = downloader.download();
		hashStorage.save(downloadFrom);
		return result;
	}

	private void stop() throws InterruptedException {
		downloader.stopDownload();
		logger.info("stop message was sent to downloader: " + downloader.isStopped());
	}

	@Override
	public void newArticle(Category newCategory, Subcategory newSubcategory, Article newArticle) {
		final FeedzillaFileCategory category = createFeedzillaCategory(newCategory);
		final FeedzillaFileSubcategory subcategory = createFeedzillaSubcategory(category, newSubcategory);
		createFeedzillaArticle(subcategory, newArticle);
	}

	private FeedzillaFileCategory createFeedzillaCategory(Category from) {
		final FeedzillaFileCategory result = new FeedzillaFileCategory(0, from.getDisplayName(), from.getEnglishName(), from.getUrlName());
		return hashStorage.createFeedzillaCategory(result);
	}

	private FeedzillaFileSubcategory createFeedzillaSubcategory(FeedzillaFileCategory category, Subcategory from) {
		final FeedzillaFileSubcategory result = new FeedzillaFileSubcategory(0, category, from.getDisplayName(), from.getEnglishName(),
				from.getUrlName());
		return hashStorage.createFeedzillaSubcategory(category, result);
	}

	private void createFeedzillaArticle(FeedzillaFileSubcategory subcategory, Article from) {
		final FeedzillaFileArticle result = new FeedzillaFileArticle(0, subcategory, from.getAuthor(), from.getPublishDate());
		result.setSource(from.getSource());
		result.setSourceUrl(from.getSourceUrl());
		result.setSummary(from.getSummary());
		result.setTitle(from.getTitle());
		result.setUrl(from.getUrl());
		hashStorage.createFeedzillaArticle(subcategory, result);
	}

	public static void main(String[] args) {
		final CountDownLatch waitForStarting = new CountDownLatch(1);
		final CountDownLatch waitForEnding = new CountDownLatch(1);
		try {
			final Thread mainProcessingThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						downloadApplication = new FeedzillaDownloadToFileApplication(DEVELOPER_FILENAME);
						waitForStarting.countDown();
						downloadApplication.start();
					} catch (Exception e) {
						logger.error("Error on main execution thread", e);
					}
					waitForEnding.countDown();
					logger.info("Execution thread is finished");
				}
			});
			mainProcessingThread.start();
			waitForStarting.await();
			logger.info("Please enter 'e' and press Enter to stop application.");
			addExitHook(waitForEnding, mainProcessingThread);
			waitForEnding.await(120, TimeUnit.SECONDS);
			mainProcessingThread.join();
			logger.info("mainProcessThread joined and life is now awesome!");
		} catch (Exception e) {
			logger.error("Error on main function. ", e);
		}
	}

	private static void addExitHook(final CountDownLatch waitForEnding, Thread mainProcessingThread) {
		try {
			try {
				final InputStreamReader fileInputStream = new InputStreamReader(System.in);
				final BufferedReader bufferedReader = new BufferedReader(fileInputStream);

				while (waitForEnding.getCount() != 0) {
					if (bufferedReader.ready()) {
						final String s = bufferedReader.readLine();
						if (s.equals("e")) {
							downloadApplication.stop();
							break;
						}
					}
					CallableArticlesDownload.pause();
				}
				bufferedReader.close();
			} catch (Exception e) {
				logger.error("Error on exit hook. ", e);
				downloadApplication.stop();
			}
		} catch (Exception e) {
			logger.error("Error on exit hook with non stop. ", e);
		}
	}
}
