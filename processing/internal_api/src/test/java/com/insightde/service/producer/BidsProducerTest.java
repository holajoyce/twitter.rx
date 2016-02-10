package com.insightde.service.producer;

import static org.junit.Assert.*;

import org.junit.Test;

public class BidsProducerTest {

	@Test
	public void testSimulation() {
		BidsProducer bp = new BidsProducer();
		bp.produceBidsSimulation();
	}

}
