/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.cdi;

import org.apache.openejb.util.reflection.Reflections;
import org.apache.webbeans.component.BuiltInOwbBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.event.EventMetadata;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ObserverMethod;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class WebappBeanManager extends BeanManagerImpl {
    private final WebappWebBeansContext webappCtx;
    private Set<Bean<?>> deploymentBeans;

    public WebappBeanManager(WebappWebBeansContext ctx) {
        super(ctx);
        webappCtx = ctx;
        deploymentBeans = super.getBeans(); // use the parent one while starting
        Reflections.set(this, "injectionResolver", new WebAppInjectionResolver(ctx));
    }

    @Override
    public void fireEvent(final Object event, final EventMetadata metadata, final boolean isLifecycleEvent) {
        final Class<?> eventClass = event.getClass();
        if(ClassUtil.isDefinitionContainsTypeVariables(ClassUtil.getClass(metadata.getType()))) {
            throw new IllegalArgumentException("Event class : " + event.getClass().getName() + " can not be defined as generic type");
        }

        getNotificationManager().fireEvent(event, metadata, isLifecycleEvent);
        if (isEvent(eventClass)) {
            getParentBm().getNotificationManager().fireEvent(event, metadata, isLifecycleEvent);
        }
    }

    @Override
    public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event, EventMetadata metadata) {
        final Class<?> eventClass = event.getClass();
        if(ClassUtil.isDefinitionContainsTypeVariables(ClassUtil.getClass(metadata.getType()))) {
            throw new IllegalArgumentException("Event type can not contain type variables. Event class is : " + eventClass);
        }

        final Set<ObserverMethod<? super T>> set = new HashSet<ObserverMethod<? super T>>();
        set.addAll(getNotificationManager().resolveObservers(event, metadata));

        if (isEvent(eventClass)) {
            set.addAll(getParentBm().getNotificationManager().resolveObservers(event, metadata));
        } // else nothing since extensions are loaded by classloader so we already have it

        return set;
    }

    @Override
    public Object getInjectableReference(InjectionPoint injectionPoint, CreationalContext<?> ctx) {
        try {
            return super.getInjectableReference(injectionPoint, ctx);
        } catch (RuntimeException e) {
            return getParentBm().getInjectableReference(injectionPoint, ctx);
        }
    }

    @Override
    public <T> CreationalContextImpl<T> createCreationalContext(Contextual<T> contextual) {
        try {
            return super.createCreationalContext(contextual);
        } catch (RuntimeException e) { // can happen?
            try {
                return getParentBm().createCreationalContext(contextual);
            } catch (RuntimeException ignored) {
                throw e;
            }
        }
    }

    @Override
    public boolean isNormalScope(Class<? extends Annotation> annotationType) {
        try {
            return super.isNormalScope(annotationType);
        } catch (RuntimeException e) {
            try {
                return getParentBm().isNormalScope(annotationType);
            } catch (RuntimeException ignored) {
                throw e;
            }
        }
    }

    @Override
    public boolean isPassivatingScope(Class<? extends Annotation> annotationType) {
        try {
            return super.isPassivatingScope(annotationType);
        } catch (RuntimeException e) {
            try {
                return getParentBm().isPassivatingScope(annotationType);
            } catch (RuntimeException ignored) {
                throw e;
            }
        }
    }


    @Override
    public boolean isQualifier(Class<? extends Annotation> annotationType) {
        try {
            return super.isQualifier(annotationType);
        } catch (RuntimeException e) {
            try {
                return getParentBm().isQualifier(annotationType);
            } catch (RuntimeException ignored) {
                throw e;
            }
        }
    }

    @Override
    public boolean isInterceptorBinding(Class<? extends Annotation> annotationType) {
        try {
            return super.isInterceptorBinding(annotationType);
        } catch (RuntimeException e) {
            try {
                return getParentBm().isInterceptorBinding(annotationType);
            } catch (RuntimeException ignored) {
                throw e;
            }
        }
    }


    @Override
    public boolean isStereotype(Class<? extends Annotation> annotationType) {
        try {
            return super.isStereotype(annotationType);
        } catch (RuntimeException e) {
            try {
                return getParentBm().isStereotype(annotationType);
            } catch (RuntimeException ignored) {
                throw e;
            }
        }
    }

    @Override
    public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> qualifier) {
        try {
            return super.getInterceptorBindingDefinition(qualifier);
        } catch (RuntimeException e) {
            try {
                return getParentBm().getInterceptorBindingDefinition(qualifier);
            } catch (RuntimeException ignored) {
                throw e;
            }
        }
    }

    @Override
    public Context getContext(Class<? extends Annotation> scope) {
        try {
            return super.getContext(scope);
        } catch (RuntimeException e) {
            try {
                return getParentBm().getContext(scope);
            } catch (RuntimeException ignored) {
                throw e;
            }
        }
    }

    @Override
    public ELResolver getELResolver() {
        return new WebAppElResolver(super.getELResolver(), getParentBm().getELResolver());
    }

    @Override
    public <T> AnnotatedType<T> createAnnotatedType(Class<T> type) {
        try {
            return super.createAnnotatedType(type);
        } catch (RuntimeException e) {
            try {
                return getParentBm().createAnnotatedType(type);
            } catch (RuntimeException ignored) {
                throw e;
            }
        }
    }

    @Override
    public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type) {
        try {
            return super.createInjectionTarget(type);
        } catch (RuntimeException e) {
            try {
                return getParentBm().createInjectionTarget(type);
            } catch (RuntimeException ignored) {
                throw e;
            }
        }
    }

    @Override
    public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory) {
        return super.wrapExpressionFactory(expressionFactory);
    }

    public BeanManagerImpl getParentBm() {
        return webappCtx.getParent().getBeanManagerImpl();
    }

    @Override
    public boolean isInUse() {
        return super.isInUse() || getParentBm().isInUse();
    }

    @Override
    public Set<Bean<?>> getComponents() {
        if (deploymentBeans.isEmpty()) {
            // probably not yet merged (afterStart())
            // so reuse parent beans
            // this can happen for validations
            return super.getBeans();
        }
        return deploymentBeans;
    }

    @Override
    public Set<Bean<?>> getBeans() {
        return deploymentBeans;
    }

    @Override
    public Bean<?> getPassivationCapableBean(final String id) {
        final Bean<?> bean = super.getPassivationCapableBean(id);
        if (bean == null) {
            return getParentBm().getPassivationCapableBean(id);
        }
        return bean;
    }

    public void afterStart() {
        deploymentBeans = new CopyOnWriteArraySet<Bean<?>>(); // override parent one with a "webapp" bean list
        for (final Bean<?> bean : getParentBm().getBeans()) {
            if (!BuiltInOwbBean.class.isInstance(bean)) {
                deploymentBeans.add(bean);
            }
        }
        deploymentBeans.addAll(super.getBeans());

        webappCtx.getBeanManagerImpl().getInjectionResolver().clearCaches(); // to force new resolution with new beans
    }

    public void beforeStop() {
        // no-op
    }

    private static boolean isEvent(final Class<?> eventClass) {
        return !WebBeansUtil.isDefaultExtensionBeanEventType(eventClass) && !WebBeansUtil.isExtensionEventType(eventClass);
    }
}
