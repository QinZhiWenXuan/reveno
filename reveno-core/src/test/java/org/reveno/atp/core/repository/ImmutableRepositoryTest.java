/** 
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.core.repository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reveno.atp.api.domain.WriteableRepository;

import java.util.ArrayList;
import java.util.List;

public class ImmutableRepositoryTest {

	private WriteableRepository underlyingRepository;
	private ImmutableModelRepository repository;
	
	@Before
	public void setUp() {
		underlyingRepository = new HashMapRepository();
		repository = new ImmutableModelRepository(underlyingRepository);
	}
	
	@Test
	public void testRollback() {
		Bin item1 = new Bin("item1", "value1");
		Bin item2 = new Bin("item2", "value2");
		
		repository.begin();
		repository.store(1L, item1);
		repository.store(2L, item2);
		
		Assert.assertTrue(repository.get(Bin.class, 1L).isPresent());
		Assert.assertTrue(repository.get(Bin.class, 2L).isPresent());
		Assert.assertFalse(repository.get(Bin.class, 3L).isPresent());
		
		Assert.assertFalse(underlyingRepository.get(Bin.class, 1L).isPresent());
		Assert.assertFalse(underlyingRepository.get(Bin.class, 2L).isPresent());
		
		repository.rollback();
		
		Assert.assertFalse(underlyingRepository.get(Bin.class, 1L).isPresent());
		Assert.assertFalse(underlyingRepository.get(Bin.class, 2L).isPresent());
		Assert.assertFalse(repository.get(Bin.class, 1L).isPresent());
		Assert.assertFalse(repository.get(Bin.class, 2L).isPresent());
	}
	
	@Test
	public void testCommit() {
		Bin item1 = new Bin("item1", "value1");
		Bin item2 = new Bin("item2", "value2");
		
		repository.begin();
		repository.store(1L, item1);
		repository.store(2L, item2);
		
		Assert.assertTrue(repository.get(Bin.class, 1L).isPresent());
		Assert.assertTrue(repository.get(Bin.class, 2L).isPresent());
		Assert.assertFalse(underlyingRepository.get(Bin.class, 1L).isPresent());
		Assert.assertFalse(underlyingRepository.get(Bin.class, 2L).isPresent());
		
		repository.commit();
		
		Assert.assertTrue(repository.get(Bin.class, 1L).isPresent());
		Assert.assertTrue(repository.get(Bin.class, 2L).isPresent());
		Assert.assertTrue(underlyingRepository.get(Bin.class, 1L).isPresent());
		Assert.assertTrue(underlyingRepository.get(Bin.class, 2L).isPresent());
	}
	
	@Test
	public void testTwoEntities() {
		Bin item1 = new Bin("item1", "value1");
		Bin item2 = new Bin("item2", "value2");
		
		repository.begin();
		repository.store(1L, item1);
		repository.store(2L, item2);
		
		Record rec = new Record();
		repository.store(1L, rec);
		Assert.assertTrue(repository.get(Bin.class, 1L).isPresent());
		Assert.assertTrue(repository.get(Bin.class, 2L).isPresent());
		Assert.assertTrue(repository.get(Record.class, 1L).isPresent());
		repository.commit();
		
		Assert.assertTrue(underlyingRepository.get(Bin.class, 1L).isPresent());
		Assert.assertTrue(underlyingRepository.get(Bin.class, 2L).isPresent());
		Assert.assertTrue(underlyingRepository.get(Record.class, 1L).isPresent());
		
		repository.begin();
		repository.store(1L, rec.addBin(1L));
		repository.rollback();
		
		Assert.assertEquals(0, repository.get(Record.class, 1L).get().bins.size());
		
		repository.begin();
		rec = repository.store(1L, rec.addBin(1L));
		rec = repository.store(1L, rec.addBin(2L));
		repository.commit();
		
		Assert.assertEquals(2, repository.get(Record.class, 1L).get().bins.size());
		Assert.assertArrayEquals(new Long[] {1L, 2L}, repository.get(Record.class, 1L).get().bins.toArray());
		
		repository.begin();
		rec = repository.store(1L, rec.removeBin(1L));
		repository.commit();
		
		Assert.assertEquals(1, repository.get(Record.class, 1L).get().bins.size());
		Assert.assertArrayEquals(new Long[] {2L}, repository.get(Record.class, 1L).get().bins.toArray());
		
		repository.store(1L, rec.addBin(1L));
		
		repository.begin();
		Assert.assertEquals(2, repository.getEntities(Bin.class).size());
		repository.store(3L, new Bin("artem", "test"));
		Assert.assertEquals(3, repository.getEntities(Bin.class).size());
		repository.rollback();
		Assert.assertEquals(2, repository.getEntities(Bin.class).size());
		
		repository.begin();
		Assert.assertEquals(2, repository.getEntities(Bin.class).size());
		repository.store(3L, new Bin("artem", "test"));
		Assert.assertEquals(3, repository.getEntities(Bin.class).size());
		repository.commit();
		Assert.assertEquals(3, repository.getEntities(Bin.class).size());
	}
	
	public static class Record {
		private final List<Long> bins;
		
		public Record addBin(long bin) {
			List<Long> nextBins = new ArrayList<>(bins);
			nextBins.add(bin);
			return new Record(nextBins);
		}
		
		public Record removeBin(long bin) {
			List<Long> nextBins = new ArrayList<>(bins);
			nextBins.remove(bin);
			return new Record(nextBins);
		}
		
		public Record() {
			this.bins = new ArrayList<>();
		}
		
		public Record(List<Long> bins) {
			this.bins = bins;
		}
	}
	
	public static class Bin {
		private String name;
		public String getName() {
			return name;
		}
		
		private Object value;
		public Object getValue() {
			return value;
		}
		
		public Bin(String name, Object value) {
			this.name = name;
			this.value = value;
		}
	}
	
}