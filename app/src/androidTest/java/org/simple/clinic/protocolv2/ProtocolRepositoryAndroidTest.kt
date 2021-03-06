package org.simple.clinic.protocolv2

import android.support.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.simple.clinic.AppDatabase
import org.simple.clinic.AuthenticationRule
import org.simple.clinic.TestClinicApp
import org.simple.clinic.TestData
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.protocol.ProtocolDrugAndDosages
import org.simple.clinic.protocol.ProtocolRepository
import org.simple.clinic.user.UserSession
import java.util.UUID
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class ProtocolRepositoryAndroidTest {

  @Inject
  lateinit var database: AppDatabase

  @Inject
  lateinit var protocolRepository: ProtocolRepository

  @Inject
  lateinit var facilityRepository: FacilityRepository

  @Inject
  lateinit var userSession: UserSession

  @Inject
  lateinit var testData: TestData

  @get:Rule
  val authenticationRule = AuthenticationRule()

  @Before
  fun setUp() {
    TestClinicApp.appComponent().inject(this)
  }

  @Test
  fun when_protocols_are_not_present_in_database_then_default_drugs_should_be_returned() {
    database.clearAllTables()

    val randomProtocolUuid = UUID.randomUUID()
    val drugs = protocolRepository.drugsForProtocolOrDefault(randomProtocolUuid).blockingFirst()
    assertThat(drugs).isEqualTo(protocolRepository.defaultProtocolDrugs())
  }

  @Test
  fun when_protocol_ID_is_null_then_default_drugs_should_be_returned() {
    val drugs = protocolRepository.drugsForProtocolOrDefault(null).blockingFirst()
    assertThat(drugs).isEqualTo(protocolRepository.defaultProtocolDrugs())
  }

  @Test
  fun when_drugs_are_not_present_for_a_protocol_then_default_values_should_be_returned() {
    database.clearAllTables()

    val protocol1 = testData.protocol()
    val protocol2 = testData.protocol()
    database.protocolDao().save(listOf(protocol1, protocol2))

    val drug1 = testData.protocolDrug(protocolUuid = protocol1.uuid)
    val drug2 = testData.protocolDrug(protocolUuid = protocol1.uuid)
    database.protocolDrugDao().save(listOf(drug1, drug2))

    val drugsForProtocol2 = protocolRepository.drugsForProtocolOrDefault(protocol2.uuid).blockingFirst()
    assertThat(drugsForProtocol2).isEqualTo(protocolRepository.defaultProtocolDrugs())
  }

  @Test
  fun when_protocols_are_present_in_database_then_they_should_be_returned() {
    database.clearAllTables()

    val currentProtocolUuid = UUID.randomUUID()

    val protocol1 = testData.protocol(uuid = currentProtocolUuid)
    val protocol2 = testData.protocol()
    database.protocolDao().save(listOf(protocol1, protocol2))

    val drug1 = testData.protocolDrug(name = "Amlodipine", protocolUuid = protocol1.uuid)
    val drug2 = testData.protocolDrug(name = "Telmisartan", protocolUuid = protocol1.uuid)
    val drug3 = testData.protocolDrug(name = "Amlodipine", protocolUuid = protocol2.uuid)
    database.protocolDrugDao().save(listOf(drug1, drug2, drug3))

    val drugsForCurrentProtocol = protocolRepository.drugsForProtocolOrDefault(currentProtocolUuid).blockingFirst()
    assertThat(drugsForCurrentProtocol).containsAllOf(
        ProtocolDrugAndDosages(drugName = "Amlodipine", drugs = listOf(drug1)),
        ProtocolDrugAndDosages(drugName = "Telmisartan", drugs = listOf(drug2)))
    assertThat(drugsForCurrentProtocol).doesNotContain(drug3)
    assertThat(drugsForCurrentProtocol).hasSize(2)
  }

  @Test
  fun protocols_drugs_should_be_grouped_by_names() {
    database.clearAllTables()

    val protocol1 = testData.protocol()
    val protocol2 = testData.protocol()
    val protocols = listOf(protocol1, protocol2)
    database.protocolDao().save(protocols)

    val amlodipine5mg = testData.protocolDrug(name = "Amlodipine", dosage = "5mg", protocolUuid = protocol1.uuid)
    val amlodipine10mg = testData.protocolDrug(name = "Amlodipine", dosage = "10mg", protocolUuid = protocol1.uuid)
    val telmisartan40mg = testData.protocolDrug(name = "Telmisartan", dosage = "40mg", protocolUuid = protocol1.uuid)
    val telmisartan80mg = testData.protocolDrug(name = "Telmisartan", dosage = "80mg", protocolUuid = protocol2.uuid)
    database.protocolDrugDao().save(listOf(amlodipine5mg, amlodipine10mg, telmisartan40mg, telmisartan80mg))

    val drugsForProtocol1 = protocolRepository.drugsForProtocolOrDefault(protocol1.uuid).blockingFirst()
    assertThat(drugsForProtocol1).containsAllOf(
        ProtocolDrugAndDosages(drugName = "Amlodipine", drugs = listOf(amlodipine5mg, amlodipine10mg)),
        ProtocolDrugAndDosages(drugName = "Telmisartan", drugs = listOf(telmisartan40mg)))
  }

  @Test
  fun protocol_drugs_received_in_an_order_should_be_returned_in_the_same_order() {
    database.clearAllTables()

    val protocolUuid = UUID.randomUUID()
    val drugPayload1 = testData.protocolDrugPayload(name = "Amlodipine", protocolUuid = protocolUuid, dosage = "5mg")
    val drugPayload2 = testData.protocolDrugPayload(name = "Telmisartan", protocolUuid = protocolUuid, dosage = "10mg")
    val drugPayload3 = testData.protocolDrugPayload(name = "Telmisartan", protocolUuid = protocolUuid, dosage = "20mg")
    val protocolPayload = testData.protocolPayload(uuid = protocolUuid, protocolDrugs = listOf(drugPayload1, drugPayload2, drugPayload3))

    val drug1 = drugPayload1.toDatabaseModel(order = 0)
    val drug2 = drugPayload2.toDatabaseModel(order = 1)
    val drug3 = drugPayload3.toDatabaseModel(order = 2)

    protocolRepository.mergeWithLocalData(listOf(protocolPayload)).blockingAwait()

    val drugsForProtocol = protocolRepository.drugsForProtocolOrDefault(protocolUuid).blockingFirst()
    assertThat(drugsForProtocol).containsAllOf(
        ProtocolDrugAndDosages(drugName = "Amlodipine", drugs = listOf(drug1)),
        ProtocolDrugAndDosages(drugName = "Telmisartan", drugs = listOf(drug2, drug3)))
  }
}
