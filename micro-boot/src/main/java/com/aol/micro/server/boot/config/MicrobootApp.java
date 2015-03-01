package com.aol.micro.server.boot.config;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import com.aol.micro.server.config.Config;
import com.aol.micro.server.module.Module;
import com.aol.micro.server.servers.ApplicationRegister;
import com.aol.micro.server.servers.ServerApplication;
import com.aol.micro.server.servers.ServerRunner;
import com.aol.micro.server.servers.model.GrizzlyApplicationFactory;
import com.aol.micro.server.spring.SpringContextFactory;
import com.aol.micro.server.spring.boot.BootApplicationConfigurator;
import com.aol.simple.react.exceptions.ExceptionSoftener;
import com.google.common.collect.Lists;


public class MicrobootApp {


	
		private final Logger logger = LoggerFactory.getLogger(this.getClass());
		private final List<Module> modules;
		private final CompletableFuture end = new CompletableFuture();
		private final ExceptionSoftener softener = ExceptionSoftener.singleton.factory.getInstance();

		@Getter
		private final ApplicationContext springContext;

		
		public MicrobootApp(Module...modules){
			this.modules = Lists.newArrayList(modules);
			springContext = new SpringContextFactory(new MicrobootConfigurator().buildConfig(extractClass()),extractClass(),
					modules[0].getSpringConfigurationClasses()).withSpringBuilder(new BootApplicationConfigurator()).createSpringContext();
			
			
		}

		public MicrobootApp(Class c, Module... modules) {

			this.modules = Lists.newArrayList(modules);
			springContext =new SpringContextFactory(new MicrobootConfigurator().buildConfig(c),c,
					modules[0].getSpringConfigurationClasses()).withSpringBuilder(new BootApplicationConfigurator()).createSpringContext();

		}

		public MicrobootApp(Config config, Module... modules) {

			this.modules = Lists.newArrayList(modules);
			config.set();
			springContext = 
					new SpringContextFactory(config,
					modules[0].getSpringConfigurationClasses())
					.withSpringBuilder(new BootApplicationConfigurator())
					.createSpringContext();

		}

		
		private Class extractClass() {
			try {
				return Class.forName(new Exception().getStackTrace()[2]
						.getClassName());
			} catch (ClassNotFoundException e) {
				softener.throwSoftenedException(e);
			}
			return null; // unreachable normally
		}

		
		public void stop() {

			end.complete(true);
			Config.reset();

		}

		public void run() {
			start().forEach(thread -> join(thread));
		}

		public List<Thread> start() {

			List<ServerApplication> apps = modules
					.stream()
					.map(module -> new GrizzlyApplicationFactory(springContext,
							module).createApp()).collect(Collectors.toList());

			ServerRunner runner;
			try {

				runner = new ServerRunner(
						springContext.getBean(ApplicationRegister.class), apps, end);
			} catch (BeansException e) {
				runner = new ServerRunner(apps, end);
			}

			return runner.run();
			
		}

		private void join(Thread thread) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}

		
		

}
