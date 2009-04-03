/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.horizon.api.tree;

import org.horizon.Cache;
import org.horizon.config.Configuration;
import org.horizon.invocation.Flag;
import org.horizon.test.MultipleCacheManagersTest;
import org.horizon.tree.Fqn;
import org.horizon.tree.Node;
import org.horizon.tree.TreeCache;
import org.horizon.tree.TreeCacheImpl;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:manik AT jboss DOT org">Manik Surtani (manik AT jboss DOT org)</a>
 */
@Test(groups = "functional", testName = "api.tree.SyncReplTest")
public class SyncReplTest extends MultipleCacheManagersTest {
   private TreeCache<Object, Object> cache1, cache2;

   protected void createCacheManagers() throws Throwable {
      Configuration c = getDefaultClusteredConfig(Configuration.CacheMode.REPL_SYNC);
      c.setInvocationBatchingEnabled(true);

      createClusteredCaches(2, "replSync", c);

      Cache c1 = cache(0, "replSync");
      Cache c2 = cache(1, "replSync");

      cache1 = new TreeCacheImpl<Object, Object>(c1);
      cache2 = new TreeCacheImpl<Object, Object>(c2);
   }

   public void testBasicOperation() {
      assertClusterSize("Should only be 2  caches in the cluster!!!", 2);

      Fqn f = Fqn.fromString("/test/data");
      String k = "key", v = "value";

      assertNull("Should be null", cache1.getRoot().getChild(f));
      assertNull("Should be null", cache2.getRoot().getChild(f));

      Node<Object, Object> node = cache1.getRoot().addChild(f);

      assertNotNull("Should not be null", node);

      node.put(k, v);

      assertEquals(v, node.get(k));
      assertEquals(v, cache1.get(f, k));
      assert v.equals(cache2.get(f, k));
   }

   public void testSyncRepl() {
      assertClusterSize("Should only be 2  caches in the cluster!!!", 2);

      Fqn fqn = Fqn.fromString("/JSESSIONID/1010.10.5:3000/1234567890/1");
      cache1.getCache().getConfiguration().setSyncCommitPhase(true);
      cache2.getCache().getConfiguration().setSyncCommitPhase(true);


      cache1.put(fqn, "age", 38);
      assertEquals("Value should be set", 38, cache1.get(fqn, "age"));
      assertEquals("Value should have replicated", 38, cache2.get(fqn, "age"));
   }

   public void testPutMap() {
      assertClusterSize("Should only be 2  caches in the cluster!!!", 2);

      Fqn fqn = Fqn.fromString("/JSESSIONID/10.10.10.5:3000/1234567890/1");
      Fqn fqn1 = Fqn.fromString("/JSESSIONID/10.10.10.5:3000/1234567890/2");

      Map<Object, Object> map = new HashMap<Object, Object>();
      map.put("1", "1");
      map.put("2", "2");
      cache1.getRoot().addChild(fqn).putAll(map, Flag.SKIP_LOCKING);
      assertEquals("Value should be set", "1", cache1.get(fqn, "1", Flag.SKIP_LOCKING));

      map = new HashMap<Object, Object>();
      map.put("3", "3");
      map.put("4", "4");
      cache1.getRoot().addChild(fqn1).putAll(map, Flag.SKIP_LOCKING);

      assertEquals("Value should be set", "2", cache1.get(fqn, "2", Flag.SKIP_LOCKING));
   }
}
