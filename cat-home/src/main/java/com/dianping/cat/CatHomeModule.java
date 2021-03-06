package com.dianping.cat;

import java.io.File;

import org.unidal.helper.Threads;
import org.unidal.initialization.AbstractModule;
import org.unidal.initialization.Module;
import org.unidal.initialization.ModuleContext;
import org.unidal.lookup.annotation.Named;

import com.dianping.cat.analysis.MessageConsumer;
import com.dianping.cat.analysis.TcpSocketReceiver;
import com.dianping.cat.config.server.ServerConfigManager;
import com.dianping.cat.consumer.CatConsumerModule;
import com.dianping.cat.report.alert.business.BusinessAlert;
import com.dianping.cat.report.alert.event.EventAlert;
import com.dianping.cat.report.alert.exception.ExceptionAlert;
import com.dianping.cat.report.alert.heartbeat.HeartbeatAlert;
import com.dianping.cat.report.alert.transaction.TransactionAlert;
import com.dianping.cat.report.page.ConfigReloadTask;
import com.dianping.cat.report.task.DefaultTaskConsumer;

@Named(type = Module.class, value = CatHomeModule.ID)
public class CatHomeModule extends AbstractModule {
	public static final String ID = "cat-home";

	@Override
	protected void execute(ModuleContext ctx) throws Exception {
		ServerConfigManager serverConfigManager = ctx.lookup(ServerConfigManager.class);

		ctx.lookup(MessageConsumer.class);

		ConfigReloadTask configReloadTask = ctx.lookup(ConfigReloadTask.class);
		Threads.forGroup("cat").start(configReloadTask);

		if (serverConfigManager.isJobMachine()) {
			DefaultTaskConsumer taskConsumer = ctx.lookup(DefaultTaskConsumer.class);

			Threads.forGroup("cat").start(taskConsumer);
		}

		if (serverConfigManager.isAlertMachine()) {
			TransactionAlert transactionAlert = ctx.lookup(TransactionAlert.class);
			EventAlert eventAlert = ctx.lookup(EventAlert.class);
			BusinessAlert metricAlert = ctx.lookup(BusinessAlert.class);
			ExceptionAlert exceptionAlert = ctx.lookup(ExceptionAlert.class);
			HeartbeatAlert heartbeatAlert = ctx.lookup(HeartbeatAlert.class);

			//ThirdPartyAlert thirdPartyAlert = ctx.lookup(ThirdPartyAlert.class);
			//ThirdPartyAlertBuilder alertBuildingTask = ctx.lookup(ThirdPartyAlertBuilder.class);

			// DatabaseAlert databaseAlert = ctx.lookup(DatabaseAlert.class);
			// SystemAlert systemAlert = ctx.lookup(SystemAlert.class);
			// NetworkAlert networkAlert = ctx.lookup(NetworkAlert.class);
			// FrontEndExceptionAlert frontEndExceptionAlert = ctx.lookup(FrontEndExceptionAlert.class);
			// AppAlert appAlert = ctx.lookup(AppAlert.class);
			// WebAlert webAlert = ctx.lookup(WebAlert.class);
			// StorageSQLAlert storageDatabaseAlert = ctx.lookup(StorageSQLAlert.class);
			// StorageCacheAlert storageCacheAlert = ctx.lookup(StorageCacheAlert.class);

			Threads.forGroup("cat").start(metricAlert);
			Threads.forGroup("cat").start(exceptionAlert);
			Threads.forGroup("cat").start(heartbeatAlert);
			Threads.forGroup("cat").start(transactionAlert);
			Threads.forGroup("cat").start(eventAlert);
			//Threads.forGroup("cat").start(thirdPartyAlert);
			//Threads.forGroup("cat").start(alertBuildingTask);
			// Threads.forGroup("cat").start(appAlert);
			// Threads.forGroup("cat").start(webAlert);
			// Threads.forGroup("cat").start(storageDatabaseAlert);
			// Threads.forGroup("cat").start(storageCacheAlert);
			// Threads.forGroup("cat").start(frontEndExceptionAlert);
			// Threads.forGroup("cat").start(networkAlert);
			// Threads.forGroup("cat").start(databaseAlert);
			// Threads.forGroup("cat").start(systemAlert);

		}

		final MessageConsumer consumer = ctx.lookup(MessageConsumer.class);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				consumer.doCheckpoint();
			}
		});
	}

	@Override
	public Module[] getDependencies(ModuleContext ctx) {
		return ctx.getModules(CatConsumerModule.ID);
	}

	@Override
	protected void setup(ModuleContext ctx) throws Exception {
		if (!isInitialized()) {
			File serverConfigFile = ctx.getAttribute("cat-server-config-file");
			ServerConfigManager serverConfigManager = ctx.lookup(ServerConfigManager.class);
			final TcpSocketReceiver messageReceiver = ctx.lookup(TcpSocketReceiver.class);

			serverConfigManager.initialize(serverConfigFile);
			messageReceiver.init();

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					messageReceiver.destory();
				}
			});
		}
	}

}
