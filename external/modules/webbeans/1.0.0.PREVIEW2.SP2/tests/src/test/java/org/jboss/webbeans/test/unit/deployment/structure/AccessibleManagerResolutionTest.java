package org.jboss.webbeans.test.unit.deployment.structure;

import org.jboss.webbeans.BeanIdStore;
import org.jboss.webbeans.BeanManagerImpl;
import org.jboss.webbeans.bean.RIBean;
import org.jboss.webbeans.bean.SimpleBean;
import org.jboss.webbeans.bootstrap.BeanDeployerEnvironment;
import org.jboss.webbeans.bootstrap.api.ServiceRegistry;
import org.jboss.webbeans.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.webbeans.ejb.EjbDescriptorCache;
import org.jboss.webbeans.introspector.WBClass;
import org.jboss.webbeans.introspector.jlr.WBClassImpl;
import org.jboss.webbeans.metadata.TypeStore;
import org.jboss.webbeans.metadata.cache.MetaAnnotationStore;
import org.jboss.webbeans.resources.ClassTransformer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AccessibleManagerResolutionTest
{
   
   private ClassTransformer classTransformer;
   private ServiceRegistry services;
   
   @BeforeMethod
   public void beforeMethod()
   {
      this.classTransformer = new ClassTransformer(new TypeStore());
      this.services = new SimpleServiceRegistry();
      this.services.add(MetaAnnotationStore.class, new MetaAnnotationStore(classTransformer));
      this.services.add(BeanIdStore.class, new BeanIdStore());
   }
   
   private void addBean(BeanManagerImpl manager, Class<?> c)
   {
      WBClass<?> clazz = WBClassImpl.of(c, classTransformer);
      RIBean<?> bean = SimpleBean.of(clazz, manager);
      manager.addBean(bean);
      BeanDeployerEnvironment environment = new BeanDeployerEnvironment(new EjbDescriptorCache(), manager);
      bean.initialize(environment);
   }
   
   @Test
   public void testAccessibleSingleLevel()
   {
      BeanManagerImpl root = BeanManagerImpl.newRootManager(services);
      BeanManagerImpl child = BeanManagerImpl.newRootManager(services);
      addBean(root, Cow.class);
      assert root.getBeans(Cow.class).size() == 1;
      assert child.getBeans(Cow.class).size() == 0;
      child.addAccessibleBeanManager(root);
      assert child.getBeans(Cow.class).size() == 1;
      addBean(child, Chicken.class);
      assert child.getBeans(Chicken.class).size() == 1;
      assert root.getBeans(Chicken.class).size() == 0;
   }
   
   @Test
   public void testAccessibleThreeLevelsWithMultiple()
   {
      BeanManagerImpl root = BeanManagerImpl.newRootManager(services);
      BeanManagerImpl child = BeanManagerImpl.newRootManager(services);
      BeanManagerImpl child1 = BeanManagerImpl.newRootManager(services);
      BeanManagerImpl grandchild = BeanManagerImpl.newRootManager(services);
      BeanManagerImpl greatGrandchild = BeanManagerImpl.newRootManager(services);
      child.addAccessibleBeanManager(root);
      grandchild.addAccessibleBeanManager(child1);
      grandchild.addAccessibleBeanManager(child);
      addBean(greatGrandchild, Cat.class);
      greatGrandchild.addAccessibleBeanManager(grandchild);
      addBean(root, Cow.class);
      addBean(child, Chicken.class);
      addBean(grandchild, Pig.class);
      addBean(child1, Horse.class);
      assert root.getBeans(Pig.class).size() == 0;
      assert root.getBeans(Chicken.class).size() == 0;
      assert root.getBeans(Cow.class).size() == 1;
      assert root.getBeans(Horse.class).size() == 0;
      assert root.getBeans(Cat.class).size() == 0;
      assert child.getBeans(Pig.class).size() == 0;
      assert child.getBeans(Chicken.class).size() == 1;
      assert child.getBeans(Cow.class).size() == 1;
      assert child.getBeans(Horse.class).size() == 0;
      assert child.getBeans(Cat.class).size() == 0;
      assert child1.getBeans(Cow.class).size() == 0;
      assert child1.getBeans(Horse.class).size() == 1;
      assert grandchild.getBeans(Pig.class).size() == 1;
      assert grandchild.getBeans(Chicken.class).size() == 1;
      assert grandchild.getBeans(Cow.class).size() == 1;
      assert grandchild.getBeans(Horse.class).size() ==1;
      assert grandchild.getBeans(Cat.class).size() == 0;
      assert greatGrandchild.getBeans(Pig.class).size() == 1;
      assert greatGrandchild.getBeans(Chicken.class).size() == 1;
      assert greatGrandchild.getBeans(Cow.class).size() == 1;
      assert greatGrandchild.getBeans(Horse.class).size() ==1;
      assert greatGrandchild.getBeans(Cat.class).size() == 1;
   }
   
   @Test
   public void testSameManagerAddedTwice()
   {
      BeanManagerImpl root = BeanManagerImpl.newRootManager(services);
      BeanManagerImpl child = BeanManagerImpl.newRootManager(services);
      BeanManagerImpl grandchild = BeanManagerImpl.newRootManager(services);
      grandchild.addAccessibleBeanManager(child);
      child.addAccessibleBeanManager(root);
      grandchild.addAccessibleBeanManager(root);
      addBean(root, Cow.class);
      addBean(child, Chicken.class);
      addBean(grandchild, Pig.class);
      assert root.getBeans(Pig.class).size() == 0;
      assert root.getBeans(Chicken.class).size() == 0;
      assert root.getBeans(Cow.class).size() == 1;
      assert child.getBeans(Pig.class).size() == 0;
      assert child.getBeans(Chicken.class).size() == 1;
      assert child.getBeans(Cow.class).size() == 1;
      assert grandchild.getBeans(Pig.class).size() == 1;
      assert grandchild.getBeans(Chicken.class).size() == 1;
      assert grandchild.getBeans(Cow.class).size() == 1;
   }
   
   @Test
   public void testCircular()
   {
      BeanManagerImpl root = BeanManagerImpl.newRootManager(services);
      BeanManagerImpl child = BeanManagerImpl.newRootManager(services);
      BeanManagerImpl grandchild = BeanManagerImpl.newRootManager(services);
      grandchild.addAccessibleBeanManager(child);
      child.addAccessibleBeanManager(root);
      grandchild.addAccessibleBeanManager(root);
      root.addAccessibleBeanManager(grandchild);
      addBean(root, Cow.class);
      addBean(child, Chicken.class);
      addBean(grandchild, Pig.class);
      assert root.getBeans(Pig.class).size() == 1;
      assert root.getBeans(Chicken.class).size() == 1;
      assert root.getBeans(Cow.class).size() == 1;
      assert child.getBeans(Pig.class).size() == 1;
      assert child.getBeans(Chicken.class).size() == 1;
      assert child.getBeans(Cow.class).size() == 1;
      assert grandchild.getBeans(Pig.class).size() == 1;
      assert grandchild.getBeans(Chicken.class).size() == 1;
      assert grandchild.getBeans(Cow.class).size() == 1;
   }
   
}
