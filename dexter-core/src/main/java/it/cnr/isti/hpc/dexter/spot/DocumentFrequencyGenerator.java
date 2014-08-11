/**
 *  Copyright 2014 Diego Ceccarelli
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package it.cnr.isti.hpc.dexter.spot;

import it.cnr.isti.hpc.dexter.shingle.Shingle;
import it.cnr.isti.hpc.dexter.shingle.ShingleExtractor;
import it.cnr.isti.hpc.dexter.spot.clean.SpotManager;
import it.cnr.isti.hpc.io.Serializer;
import it.cnr.isti.hpc.log.ProgressLogger;
import it.cnr.isti.hpc.structure.LRUCache;
import it.cnr.isti.hpc.wikipedia.article.Article;
import it.unimi.dsi.util.BloomFilter;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * @author Diego Ceccarelli <diego.ceccarelli@isti.cnr.it>
 * 
 *         Created on Aug 8, 2014
 */
public class DocumentFrequencyGenerator {

	BloomFilter<Void> bf = BloomFilter.create(10000000L);
	SpotManager spotManager = SpotManager.getStandardSpotManager();
	LRUCache<String, Set<String>> cache = new LRUCache<String, Set<String>>(
			100000);

	public DocumentFrequencyGenerator(Iterator<String> spotSrcTargetIterator) {
		File bloom = new File("/tmp/bf.bin");
		if (!bloom.exists()) {
			initBloomFilter(spotSrcTargetIterator);
			Serializer serializer = new Serializer();
			serializer.dump(bf, bloom.getAbsolutePath());
		} else {
			Serializer serializer = new Serializer();
			bf = (BloomFilter<Void>) serializer.load(bloom.getAbsolutePath());
		}
	}

	private void initBloomFilter(Iterator<String> spotIterator) {
		String spot = spotIterator.next();
		bf.add(spot);
		ProgressLogger pl = new ProgressLogger(
				"added {} spots to the bloom filter", 10000);
		pl.up();
		while (spotIterator.hasNext()) {
			String next = spotIterator.next();
			if (next.equals(spot))
				continue;
			pl.up();
			spot = next;
			bf.add(spot);
		}

	}

	public Multiset<String> getSpotsAndFrequencies(Article a) {
		Multiset<String> freqs = HashMultiset.create();
		ShingleExtractor se = new ShingleExtractor(a);

		for (Shingle shingle : se) {
			String key = shingle.getText();
			Set<String> set = cache.get(key);
			if (set == null) {
				set = new HashSet<String>();
				for (String s : spotManager.process(shingle.getText())) {

					if (bf.contains(s)) {
						set.add(s);
					}

				}
				cache.put(key, set);

			}
			for (String spot : set) {
				freqs.add(spot);
			}

		}
		return freqs;

	}

}
