package com.github.mkopylec.sessioncouchbase.specification.session

import com.github.mkopylec.sessioncouchbase.Message
import org.springframework.test.context.ActiveProfiles

import static com.github.mkopylec.sessioncouchbase.assertions.Assertions.assertThat

@ActiveProfiles('persistent')
class PersistentSessionSpec extends SessionSpec {

    def "Should copy HTTP session attributes when session ID was changed"() {
        given:
        def message = new Message(text: 'i cannot disappear!', number: 13)
        setSessionAttribute message
        def globalMessage = new Message(text: 'i cannot disappear too!', number: 12222)
        setGlobalSessionAttribute globalMessage
        startExtraApplicationInstance('different-namespace')
        def extraMessage = new Message(text: 'and me too!', number: 14100)
        setSessionAttributeToExtraInstance extraMessage

        when:
        changeSessionId()

        then:
        assertThat(getSessionAttribute())
                .hasBody(message)
        assertThat(getGlobalSessionAttributeFromExtraInstance())
                .hasBody(globalMessage)
        assertThat(getSessionAttributeFromExtraInstance())
                .hasBody(extraMessage)
    }
}