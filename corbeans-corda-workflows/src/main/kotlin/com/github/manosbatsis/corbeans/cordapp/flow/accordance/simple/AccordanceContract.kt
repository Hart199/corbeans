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
package com.github.manosbatsis.corbeans.cordapp.flow.accordance.simple

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

/**
 * Minimal contract to encode a simple workflow with one initial status and two possible eventual states.
 * It is assumed one party unilaterally submits and the other manually reviews the data and completes it.
 */
data class AccordanceContract(val blank: Unit? = null) : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_ID = "com.github.manosbatsis.corbeans.corda.workflows.accordance.simple.AccordanceContract"
    }


    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands  // Record receipt of deal details
        class Completed : TypeOnlyCommandData(), Commands  // Record match
    }

    /**
     * The verify method locks down the allowed transactions to contain just a single proposal being
     * created/modified and the only modification allowed is to the status field.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        requireNotNull(tx.timeWindow) { "must have a time-window" }
        when (command.value) {
            is Commands.Issue -> {
                requireThat {
                    "Issue of new WorkflowContract must not include any inputs" using (tx.inputs.isEmpty())
                    "Issue of new WorkflowContract must be in a unique transaction" using (tx.outputs.size == 1)
                }
                val issued = tx.outputsOfType<AccordanceState>().single()
                requireThat {
                    "Issue requires the initiatingParty Party as signer" using (command.signers.contains(issued.initiatingParty.owningKey))
                    "Initial Issue status must be NEW" using (issued.status == AccordanceStatus.NEW)
                }
            }
            is Commands.Completed -> {
                val stateGroups = tx.groupStates(AccordanceState::class.java) { it.linearId }
                require(stateGroups.size == 1) { "Must be only a single proposal in transaction" }
                for ((inputs, outputs) in stateGroups) {
                    val before = inputs.single()
                    val after = outputs.single()
                    requireThat {
                        "Only a non-final dataAccordance can be modified" using (before.status == AccordanceStatus.NEW)
                        "Output must be a final status" using (after.status in setOf(AccordanceStatus.APPROVED, AccordanceStatus.REJECTED))
                        "Completed command can only change status" using (before == after.copy(status = before.status))
                        "Completed command requires the initiatingParty Party as signer" using (command.signers.contains(before.initiatingParty.owningKey))
                        "Completed command requires the counterParty as signer" using (command.signers.contains(before.counterParty.owningKey))
                    }
                }
            }
            else -> throw IllegalArgumentException("Unrecognised Command $command")
        }
    }
}