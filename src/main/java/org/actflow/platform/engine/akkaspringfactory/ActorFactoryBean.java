package org.actflow.platform.engine.akkaspringfactory;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.japi.Creator;
import akka.routing.RouterConfig;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Factory bean that creates actors with prototype scope for given actor class.
 * This factory supports autowiring of created beans.
 *
 * @author Davis Lau
 */
public class ActorFactoryBean implements FactoryBean<ActorRef>, ApplicationContextAware {

	private ApplicationContext ctx;
	private ActorSystem actorSystem;
	private String actorClass;
	private Object[] args = {};
	private String actorBeanName;
	private RouterConfig routerConfig;
	private Deploy deploy;
	private String mailbox;
	private String dispatcher;
	private boolean withRouter = false;

	@Override
	public ActorRef getObject() throws Exception {
		return doCreateObject();
	}

	private ActorRef doCreateObject() throws Exception {
		Props props = null;
		if (actorClass != null) {
			props = Props.create(new SpringCreator(ctx, Class.forName(actorClass), args));
		} else if (actorBeanName != null) {
			props = SpringProps.create(withRouter, actorSystem, actorBeanName);
		}

		if (props == null) {
			throw new BeanCreationException("Can not create ActorRef for given parameters, actorClass=" + actorClass + ", actorBeanName=" + actorBeanName);
		}

		if (routerConfig != null) {
			props = props.withRouter(routerConfig);
		}
		if (deploy != null) {
			props = props.withDeploy(deploy);
		}
		if (mailbox != null) {
			props = props.withMailbox(mailbox);
		}
		if (dispatcher != null) {
			props = props.withDispatcher(dispatcher);
		}
		
//		getContext()
//		
//		actorSystem.get

		return actorSystem.actorOf(props, actorBeanName);
	}

	@Override
	public Class<?> getObjectType() {
		return ActorRef.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.ctx = applicationContext;
	}

	public void setActorSystem(ActorSystem actorSystem) {
		this.actorSystem = actorSystem;
	}

	public void setActorBeanName(String actorBeanName) {
		this.actorBeanName = actorBeanName;
	}

	public void setRouterConfig(RouterConfig routerConfig) {
		this.routerConfig = routerConfig;
	}

	public void setDeploy(Deploy deploy) {
		this.deploy = deploy;
	}

	public void setMailbox(String mailbox) {
		this.mailbox = mailbox;
	}

	public void setDispatcher(String dispatcher) {
		this.dispatcher = dispatcher;
	}

	public void setActorClass(String actorClass) {
		this.actorClass = actorClass;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public void setWithRouter(boolean withRouter) {
		this.withRouter = withRouter;
	}

	private static class SpringCreator implements Creator<Actor> {
		private static final long serialVersionUID = 1L;

		// TODO what about serializability ?
		private ApplicationContext ctx;
		private Class<?> clazz;
		// TODO what about serializability ?
		private Object[] args;

		private SpringCreator(ApplicationContext ctx, Class<?> clazz, Object... args) {
			this.ctx = ctx;
			this.clazz = clazz;
			this.args = args;
		}

		@Override
		public Actor create() throws Exception {
			Actor actor = (Actor) ConstructorUtils.invokeConstructor(clazz, args);
			if(actor != null) {
				ctx.getAutowireCapableBeanFactory().autowireBean(actor);
			}
			return actor;
		}
	}
}
