import db.DBOperator
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DBOperatorTests {

    @BeforeEach
    fun setup() {
        mockkObject(DBOperator)
        every { DBOperator.connectOrCreate(any()) } just runs
        every { DBOperator.createDBForTests(any()) } just runs
    }

//    @Test
//    fun `test addTexture`() {
//        val pathToFile = "test_texture.jpg"
//
//        // Mock DBOperator.addTexture
//        every { DBOperator.addTexture(any()) } returns mockk {
//            every { raw() } returns TextureInfo(id = 1u, pathToFile = pathToFile)
//        }
//
//        // Execute the function you want to test
//        val texture = DBOperator.addTexture(pathToFile)
//
//        // Assertions
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
