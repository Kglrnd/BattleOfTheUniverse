package de.kugi.dev.battleoftheuniverse.planet;

import de.kugi.dev.battleoftheuniverse.planet.dto.SlotStatus;
import de.kugi.dev.battleoftheuniverse.planet.dto.SystemView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class PlanetServiceTest {

    @Mock
    private PlanetRepository planetRepository;
    @Mock
    private ApplicationEventPublisher events;

    private PlanetService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PlanetService(planetRepository, events);
    }

    @Test
    void getSystemViewRejectsOutOfBoundsCoordinates() {
        assertThatThrownBy(() -> service.getSystemView(0, 1))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.getSystemView(1, SystemLayout.SYSTEMS_PER_GALAXY + 1))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getSystemViewMarksVoidOccupiedAndFreeSlotsCorrectly() {
        int galaxy = 3;
        int system = 42;
        Set<Integer> usable = SystemLayout.usablePositions(galaxy, system);
        int occupiedPosition = usable.iterator().next();
        Planet planet = new Planet("Colony", 7L, galaxy, system, occupiedPosition, PlanetClass.TEMPERATE);

        when(planetRepository.findByGalaxyAndSystem(galaxy, system)).thenReturn(List.of(planet));

        SystemView view = service.getSystemView(galaxy, system);

        assertThat(view.slots()).hasSize(SystemLayout.MAX_POSITIONS);
        for (var slot : view.slots()) {
            if (!usable.contains(slot.position())) {
                assertThat(slot.status()).isEqualTo(SlotStatus.VOID);
            } else if (slot.position() == occupiedPosition) {
                assertThat(slot.status()).isEqualTo(SlotStatus.OCCUPIED);
                assertThat(slot.planet().name()).isEqualTo("Colony");
            } else {
                assertThat(slot.status()).isEqualTo(SlotStatus.FREE);
            }
        }
    }

    @Test
    void aPlanetOnAPositionTheLayoutNoLongerConsidersUsableStillShowsAsOccupied() {
        int galaxy = 4;
        int system = 92;
        Set<Integer> usable = SystemLayout.usablePositions(galaxy, system);
        int voidPosition = SystemLayout.MAX_POSITIONS; // find a position outside the usable set
        while (usable.contains(voidPosition)) {
            voidPosition--;
        }
        Planet planet = new Planet("Legacy Homeworld", 7L, galaxy, system, voidPosition, PlanetClass.TEMPERATE);
        when(planetRepository.findByGalaxyAndSystem(galaxy, system)).thenReturn(List.of(planet));

        SystemView view = service.getSystemView(galaxy, system);

        var slot = view.slots().get(voidPosition - 1);
        assertThat(slot.status()).isEqualTo(SlotStatus.OCCUPIED);
        assertThat(slot.planet().name()).isEqualTo("Legacy Homeworld");
    }
}
