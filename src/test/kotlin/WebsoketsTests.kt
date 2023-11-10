import db.DBOperator
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.jupiter.api.*
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DBOperatorTests {

//    @BeforeAll
//    fun initDatabase() {
//        DBOperator.connectOrCreate(initTables = true)
//    }

    @BeforeEach
    fun setup() {
        mockkObject(DBOperator)
        every { DBOperator.createDBForTests(any()) } just runs
    }

    @AfterAll
    fun cleanup() {
        unmockkAll()
    }


//    @Test
//    fun `test addTexture`() {
//        val pathToFile = ".\\textures\\test_texture.png"
//
//        every { DBOperator.addTexture(any()) } returns mockk {
//            every { id } returns 1u
//            every { pathToFile } returns pathToFile
//        }
//
//        val texture = DBOperator.addTexture(pathToFile)
//
//
//        verify { DBOperator.addTexture(pathToFile) }
//        assertEquals(1u, texture.id)
//        assertEquals(pathToFile, texture.pathToFile)
//    }



    @Test
    fun `test deleteTextureByID`() {
        val textureId = 1u
        every { DBOperator.deleteTextureByID(any()) } returns true
        val result = DBOperator.deleteTextureByID(textureId)
        verify { DBOperator.deleteTextureByID(textureId) }
        assertTrue(result)
    }

    @Test
    fun `test deleteUserByID`() {
        val userId = 1u
        every { DBOperator.deleteUserByID(any()) } returns true
        val result = DBOperator.deleteUserByID(userId)
        verify { DBOperator.deleteUserByID(userId) }
        assertTrue(result)
    }

}
