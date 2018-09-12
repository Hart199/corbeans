/**
 *     Corda-Spring: integration and other utilities for developers working with Spring-Boot and Corda.
 *     Copyright (C) 2018 Manos Batsis
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */
package com.github.manosbatsis.corda.webserver.spring

import com.github.manosbatsis.corda.spring.beans.CordaNodeService
import net.corda.core.identity.Party
import net.corda.core.utilities.NetworkHostAndPort
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
class MultiNodeIntegrationTest() : AbstractSingleCordaNetworkIntegrationTest() {

    companion object {
        private val logger = LoggerFactory.getLogger(MultiNodeIntegrationTest::class.java)

    }

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var services: Map<String, CordaNodeService>

    @Autowired
    @Qualifier("partyANodeService")
    lateinit var service: CordaNodeService

    @Test
    fun `Can inject services`() {
        assertNotNull(this.services)
        assertNotNull(this.service)
        assertTrue(this.services.keys.isNotEmpty())
    }

    @Test
    fun `Can retreive node identity`() {
        assertNotNull(service.myIdentity)
    }

    @Test
    fun `Can retreive notaries`() {
        val notaries: List<Party> = service.notaries()
        assertNotNull(notaries)
    }

    @Test
    fun `Can retreive flows`() {
        val flows: List<String> = service.flows()
        assertNotNull(flows)
    }

    @Test
    fun `Can retreive addresses`() {
        val addresses: List<NetworkHostAndPort> = service.addresses()
        assertNotNull(addresses)
    }

}