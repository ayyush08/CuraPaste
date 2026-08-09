package com.curapaste.events;

import com.curapaste.entities.Paste;
import com.curapaste.repository.PasteRepository;
import com.curapaste.services.AbuseService;
import com.curapaste.services.storage.ContentStorageService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

//Consumers can be anything, since producer is not specific. This one can be considered an example
@Component
public class AbuseScanConsumer {
    private final PasteRepository pasteRepository;
    private final AbuseService abuseService;
    private final ContentStorageService contentStorageService;

    public AbuseScanConsumer(PasteRepository pasteRepository,
                             AbuseService abuseService,
                             ContentStorageService contentStorageService) {
        this.pasteRepository = pasteRepository;
        this.abuseService  = abuseService;
        this.contentStorageService = contentStorageService;
    }

    @RabbitListener(queues = "${rabbitmq.abuse-scan-queue}")
    public void consume(PasteEvent event) {
//        throw new RuntimeException("TEST DLQ");
        System.out.println("Abuse scan received for paste: " + event.getShortId());

        Paste p = pasteRepository.findAliveByShortId(event.getShortId()).orElse(null);

        if(p == null)  return;

        String content;

        if(p.getContentLocation()!=null) content = contentStorageService.fetch(p.getContentLocation());
        else content = p.getContent();

        boolean isAbuse = abuseService.isSuspicious(content);

        if(isAbuse) System.out.println("ABUSE DETECTED: SHORT ID: "+event.getShortId());
        else System.out.println("PASTE IS CLEAN: SHORT ID: "+event.getShortId());

    }
}
