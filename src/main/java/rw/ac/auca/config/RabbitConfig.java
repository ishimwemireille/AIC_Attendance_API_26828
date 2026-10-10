package rw.ac.auca.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Class RabbitConfig. The topology for project instruction 7.
 *
 *   attendance.events (topic exchange)
 *        |-- attendance.absent --> notification.email
 *        |-- attendance.absent --> notification.sms
 *
 * Both queues are durable and dead-letter to notification.dlq, so a message
 * that cannot be handled after its retries is kept rather than dropped.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "attendance.events";
    public static final String ROUTING_ABSENT = "attendance.absent";

    public static final String QUEUE_EMAIL = "notification.email";
    public static final String QUEUE_SMS = "notification.sms";

    public static final String DLQ_EXCHANGE = "attendance.dlx";
    public static final String QUEUE_DEAD = "notification.dlq";

    @Bean
    public TopicExchange attendanceExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(QUEUE_EMAIL)
                .deadLetterExchange(DLQ_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_DEAD)
                .build();
    }

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(QUEUE_SMS)
                .deadLetterExchange(DLQ_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_DEAD)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_DEAD).build();
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(attendanceExchange()).with(ROUTING_ABSENT);
    }

    @Bean
    public Binding smsBinding() {
        return BindingBuilder.bind(smsQueue()).to(attendanceExchange()).with(ROUTING_ABSENT);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(QUEUE_DEAD);
    }
}
