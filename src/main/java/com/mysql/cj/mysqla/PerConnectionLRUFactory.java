/*
  Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.

  The MySQL Connector/J is licensed under the terms of the GPLv2
  <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most MySQL Connectors.
  There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
  this software, see the FOSS License Exception
  <http://www.mysql.com/about/legal/licensing/foss-exception.html>.

  This program is free software; you can redistribute it and/or modify it under the terms
  of the GNU General Public License as published by the Free Software Foundation; version 2
  of the License.

  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along with this
  program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
  Floor, Boston, MA 02110-1301  USA

 */

package com.mysql.cj.mysqla;

import java.util.Set;

import com.mysql.cj.api.CacheAdapter;
import com.mysql.cj.api.CacheAdapterFactory;
import com.mysql.cj.core.util.LRUCache;

public class PerConnectionLRUFactory implements CacheAdapterFactory<String, ParseInfo> {

    public CacheAdapter<String, ParseInfo> getInstance(Object syncMutex, String url, int cacheMaxSize, int maxKeySize) {

        return new PerConnectionLRU(syncMutex, cacheMaxSize, maxKeySize);
    }

    class PerConnectionLRU implements CacheAdapter<String, ParseInfo> {
        private final int cacheSqlLimit;
        private final LRUCache<String, ParseInfo> cache;
        private final Object syncMutex;

        protected PerConnectionLRU(Object syncMutex, int cacheMaxSize, int maxKeySize) {
            final int cacheSize = cacheMaxSize;
            this.cacheSqlLimit = maxKeySize;
            this.cache = new LRUCache<>(cacheSize);
            this.syncMutex = syncMutex;
        }

        public ParseInfo get(String key) {
            if (key == null || key.length() > this.cacheSqlLimit) {
                return null;
            }

            synchronized (this.syncMutex) {
                return this.cache.get(key);
            }
        }

        public void put(String key, ParseInfo value) {
            if (key == null || key.length() > this.cacheSqlLimit) {
                return;
            }

            synchronized (this.syncMutex) {
                this.cache.put(key, value);
            }
        }

        public void invalidate(String key) {
            synchronized (this.syncMutex) {
                this.cache.remove(key);
            }
        }

        public void invalidateAll(Set<String> keys) {
            synchronized (this.syncMutex) {
                for (String key : keys) {
                    this.cache.remove(key);
                }
            }

        }

        public void invalidateAll() {
            synchronized (this.syncMutex) {
                this.cache.clear();
            }
        }
    }
}
