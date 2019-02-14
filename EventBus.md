##EventBus

EventBus的getDefault方法
```
static volatile EventBus defaultInstance;

public static EventBus getDefault() {
        if (defaultInstance == null) {
            synchronized (EventBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }

```
方法返回一个EventBus的单例对象

EventBus的register方法
```
public void register(Object subscriber) {
        Class<?> subscriberClass = subscriber.getClass();
        //注释1处
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
        synchronized (this) {
            //注释2处
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                subscribe(subscriber, subscriberMethod);
            }
        }
    }
```
首先在register方法的注释1处查找传入的订阅着的所有订阅方法
```
 List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
 			//首先从缓存中查找方法
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
        if (subscriberMethods != null) {
            return subscriberMethods;
        }
			//注释1处，默认情况下ignoreGeneratedIndex是false
        if (ignoreGeneratedIndex) {
            subscriberMethods = findUsingReflection(subscriberClass);
        } else {
            subscriberMethods = findUsingInfo(subscriberClass);
        }
        if (subscriberMethods.isEmpty()) {
            throw new EventBusException("Subscriber " + subscriberClass
                    + " and its super classes have no public methods with the @Subscribe annotation");
        } else {
            //把方法加入到缓存
            METHOD_CACHE.put(subscriberClass, subscriberMethods);
            return subscriberMethods;
        }
    }
```
在注释1处，默认情况下ignoreGeneratedIndex是false，所以会调用findUsingInfo方法
```
private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
        FindState findState = prepareFindState();
        findState.initForSubscriber(subscriberClass);
        while (findState.clazz != null) {
            //注释1处
            findState.subscriberInfo = getSubscriberInfo(findState);
            if (findState.subscriberInfo != null) {
                SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
                for (SubscriberMethod subscriberMethod : array) {
                    if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
                        findState.subscriberMethods.add(subscriberMethod);
                    }
                }
            } else {
                //注释2处
                findUsingReflectionInSingleClass(findState);
            }
            //注释3处
            findState.moveToSuperclass();
        }
        return getMethodsAndRelease(findState);
    }
```

在注释1处我们调用SubscriberMethodFinder的getSubscriberInfo方法。

```
private SubscriberInfo getSubscriberInfo(FindState findState) {
        if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null) {
            SubscriberInfo superclassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
            if (findState.clazz == superclassInfo.getSubscriberClass()) {
                return superclassInfo;
            }
        }
        //默认subscriberInfoIndexes是null
        if (subscriberInfoIndexes != null) {
            for (SubscriberInfoIndex index : subscriberInfoIndexes) {
                SubscriberInfo info = index.getSubscriberInfo(findState.clazz);
                if (info != null) {
                    return info;
                }
            }
        }
        return null;
    }
```
在默认subscriberInfoIndexes是null，所以getSubscriberInfo方法返回null，所以会调用注释2处的findUsingReflectionInSingleClass方法。

```
  private void findUsingReflectionInSingleClass(FindState findState) {
        Method[] methods;
        try {
            // 获取类对象声明的所有方法，包括public, protected, default (package) access, and private methods,但是不包括继承的方法。
            methods = findState.clazz.getDeclaredMethods();
        } catch (Throwable th) {
            // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
            methods = findState.clazz.getMethods();
            findState.skipSuperClasses = true;
        }
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            //如果方法修饰符是public并且不是ABSTRACT，STATIC，BRIDGE，SYNTHETIC这几种类型
            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {
            	   //获取方法参数类型数组
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1) {//如果只有一种参数类型
                	  //查找方法是否有Subscribe注解
                    Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                    if (subscribeAnnotation != null) {
                        Class<?> eventType = parameterTypes[0];
                        if (findState.checkAdd(method, eventType)) {
                            ThreadMode threadMode = subscribeAnnotation.threadMode();
                            //构建一个SubscriberMethod对象，并加入到findState.subscriberMethods中
                            findState.subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode,
                                    subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
                        }
                    }
                }
                //默认情况下strictMethodVerification为false
                 else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                    throw new EventBusException("@Subscribe method " + methodName +
                            "must have exactly 1 parameter but has " + parameterTypes.length);
                }
            } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                throw new EventBusException(methodName +
                        " is a illegal @Subscribe method: must be public, non-static, and non-abstract");
            }
        }
    }

```
findUsingReflectionInSingleClass方法就是通过反射来获取订阅者中所有的方法，并根据方法的类型、参数和注解来找到订阅方法。找到订阅方法后即哪个订阅方法的相关信息保存在findSatate中。（有点疑问，看上面的逻辑订阅方法必须是public的才行，可是在kotlin中我是用了protected方法也是可以的）

接着回到findUsingInfo方法的注释3处，会继续查找订阅者父类中的订阅方法。

继续回到EventBus的register方法的注释2处,调用subscribe方法
```
    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
        Class<?> eventType = subscriberMethod.eventType;
        //注释1处
        Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
        //注释2处，查找是否存在eventType对应的Subscription对象列表
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions == null) {//如果不存在，就新建一个Subscription对象列表，并将eventType和对应的subscriptions加入到subscriptionsByEventType中
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionsByEventType.put(eventType, subscriptions);
        } else {
        		//如果列表中存在和newSubscription一样的Subscription，则抛出异常
            if (subscriptions.contains(newSubscription)) {
                throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
                        + eventType);
            }
        }

        int size = subscriptions.size();
        for (int i = 0; i <= size; i++) {
        		//将newSubscription对象加入到subscriptions列表中，并且根据subscriberMethod的优先级排序
            if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
                subscriptions.add(i, newSubscription);
                break;
            }
        }

			//查找订阅者对应的订阅事件类型列表，如果为null，则创建新的列表，并把eventType加入到列表中，最后把subscriber和对应的subscribedEvents加入到typesBySubscriber中
        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if (subscribedEvents == null) {
            subscribedEvents = new ArrayList<>();
            typesBySubscriber.put(subscriber, subscribedEvents);
        }
        subscribedEvents.add(eventType);
			//处理黏性事件
        if (subscriberMethod.sticky) {
            if (eventInheritance) {//eventInheritance默认是true
                // Existing sticky events of all subclasses of eventType have to be considered.
                // Note: Iterating over all events may be inefficient with lots of sticky events,
                // thus data structure should be changed to allow a more efficient lookup
                // (e.g. an additional map storing sub classes of super classes: Class -> List<Class>).
                //stickyEvents的Entry对象存储的是事件类型和对应的事件
                Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
                for (Map.Entry<Class<?>, Object> entry : entries) {
                    Class<?> candidateEventType = entry.getKey();
                    //如果eventType和candidateEventType是同一个类型，或者eventType是candidateEventType的父类或者父接口
                    if (eventType.isAssignableFrom(candidateEventType)) {
                    	   //获取保存的事件，注意黏性事件是保存在内存中的，当你杀死进程以后，黏性事件就会消失。
                        Object stickyEvent = entry.getValue();
                        //注释2处，检查并发送黏性事件
                        checkPostStickyEventToSubscription(newSubscription, stickyEvent);
                    }
                }
            } else {
                Object stickyEvent = stickyEvents.get(eventType);
                checkPostStickyEventToSubscription(newSubscription, stickyEvent);
            }
        }
    }

```
检查并发送黏性事件
```
private void checkPostStickyEventToSubscription(Subscription newSubscription, Object stickyEvent) {
        if (stickyEvent != null) {
            // If the subscriber is trying to abort the event, it will fail (event is not tracked in posting state)
            // --> Strange corner case, which we don't take care of here.
            postToSubscription(newSubscription, stickyEvent, isMainThread());
        }
    }
```
如果黏性事件不为null，则发送黏性事件
```
 private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
        switch (subscription.subscriberMethod.threadMode) {
            case POSTING:
                invokeSubscriber(subscription, event);
                break;
            case MAIN:
                if (isMainThread) {
                    invokeSubscriber(subscription, event);
                } else {
                    mainThreadPoster.enqueue(subscription, event);
                }
                break;
            case MAIN_ORDERED:
                if (mainThreadPoster != null) {
                    mainThreadPoster.enqueue(subscription, event);
                } else {
                    // temporary: technically not correct as poster not decoupled from subscriber
                    invokeSubscriber(subscription, event);
                }
                break;
            case BACKGROUND:
                if (isMainThread) {
                    backgroundPoster.enqueue(subscription, event);
                } else {
                    invokeSubscriber(subscription, event);
                }
                break;
            case ASYNC:
                asyncPoster.enqueue(subscription, event);
                break;
            default:
                throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
        }
    }
```
以订阅方法的threadMode是POSTING为例，那么会调用invokeSubscriber方法
```
void invokeSubscriber(Subscription subscription, Object event) {
        try {
        	  //使用反射的方式调用订阅者的订阅方法
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
        } catch (InvocationTargetException e) {
            handleSubscriberException(subscription, event, e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }
```
invokeSubscriber方法内部就是使用反射的方式调用订阅者的订阅方法。

注册方法就分析到这里，接下来看一看事件的发送。
```
EventBus.getDefault().post(MessageEvent("hello eventbus"))

```
EventBus的post方法
```
public void post(Object event) {
        PostingThreadState postingState = currentPostingThreadState.get();
        //注释1处，将事件加入到eventQueue
        List<Object> eventQueue = postingState.eventQueue;
        eventQueue.add(event);
			//当前没有在发送事件
        if (!postingState.isPosting) {
            postingState.isMainThread = isMainThread();
            postingState.isPosting = true;
            if (postingState.canceled) {
                throw new EventBusException("Internal error. Abort state was not reset");
            }
            try {
            		//注释2处，循环发送事件，事件发送是按照加入列表的顺序来发送的，先进先出。
                while (!eventQueue.isEmpty()) {
                    postSingleEvent(eventQueue.remove(0), postingState);
                }
            } finally {
                postingState.isPosting = false;
                postingState.isMainThread = false;
            }
        }
    }
```
```
private void postSingleEvent(Object event, PostingThreadState postingState) throws Error {
        Class<?> eventClass = event.getClass();
        boolean subscriptionFound = false;
        //eventInheritance默认是true
        if (eventInheritance) {
        	  //获取eventClass对应的所有类，包括父类和父接口。
            List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
            int countTypes = eventTypes.size();
            for (int h = 0; h < countTypes; h++) {
                Class<?> clazz = eventTypes.get(h);
                //注释1处
                subscriptionFound |= postSingleEventForEventType(event, postingState, clazz);
            }
        } else {
            subscriptionFound = postSingleEventForEventType(event, postingState, eventClass);
        }
        //注释2处
        if (!subscriptionFound) {
            if (logNoSubscriberMessages) {
                logger.log(Level.FINE, "No subscribers registered for event " + eventClass);
            }
            //注释3处
            if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent.class &&
                    eventClass != SubscriberExceptionEvent.class) {
                //注释4处，post一个NoSubscriberEvent事件，最终只会在上面的注释2处打印日志然后结束
                post(new NoSubscriberEvent(this, event));
            }
        }
    }

```
EventBus的postSingleEventForEventType方法
```
private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (this) {
        	  //获取所有的订阅者
            subscriptions = subscriptionsByEventType.get(eventClass);
        }
        if (subscriptions != null && !subscriptions.isEmpty()) {
        	  //循环发送事件
            for (Subscription subscription : subscriptions) {
                postingState.event = event;
                postingState.subscription = subscription;
                boolean aborted = false;
                try {
                	  //注释1处，调用postToSubscription方法
                    postToSubscription(subscription, event, postingState.isMainThread);
                    aborted = postingState.canceled;
                } finally {
                    postingState.event = null;
                    postingState.subscription = null;
                    postingState.canceled = false;
                }
                if (aborted) {
                    break;
                }
            }
            return true;
        }
        return false;
    }
```
在注释1处调用了postToSubscription方法，在分析黏性事件的时候看过了，这里就不再看了。

EventBus的取消注册方法
```
 public synchronized void unregister(Object subscriber) {
 	 //获取订阅者订阅的所有事件类型列表
    List<Class<?>> subscribedTypes = typesBySubscriber.get(subscriber);
    if (subscribedTypes != null) {
        for (Class<?> eventType : subscribedTypes) {
        	  //注释1处
            unsubscribeByEventType(subscriber, eventType);
        }
        //移除订阅者
        typesBySubscriber.remove(subscriber);
    } else {
        logger.log(Level.WARNING, "Subscriber to unregister was not registered before: " + subscriber.getClass());
    }
}
```
在注释1处，调用unsubscribeByEventType方法移除subscriber对应的所有Subscription对象。
```
private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
        List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions != null) {
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                Subscription subscription = subscriptions.get(i);
                if (subscription.subscriber == subscriber) {
                    subscription.active = false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }
```






