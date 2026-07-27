import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';

export function NetworkCanvas({ selectedVendorId, vendors = [] }) {
  const mountRef = useRef(null);

  useEffect(() => {
    const canvas = mountRef.current;
    if (!canvas) return;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(40, 1, 0.1, 100);
    camera.position.set(0, 0.4, 10);

    const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setClearColor(0x000000, 0);

    const root = new THREE.Group();
    root.position.set(2.1, -0.1, 0);
    scene.add(root);

    scene.add(new THREE.AmbientLight(0xbce6d6, 1.2));

    const keyLight = new THREE.PointLight(0x82e3c6, 18, 18);
    keyLight.position.set(-3, 2, 5);
    scene.add(keyLight);

    const warmLight = new THREE.PointLight(0xe6a16d, 10, 14);
    warmLight.position.set(5, -2, 3);
    scene.add(warmLight);

    const nodes = [];
    const positions = [
      [0, 0, 0],
      [2.1, 1.25, -0.4],
      [2.5, -1.55, 0.25],
      [-2.25, 1.15, 0.2],
      [-2.0, -1.55, -0.5],
      [0.2, 2.35, -0.9],
      [0.35, -2.25, 0.4]
    ];
    const palette = [0x74cfbd, 0xe5a066, 0x74cfbd, 0xdb7979, 0x74cfbd, 0x74cfbd, 0xe5a066];
    const nodeGeometry = new THREE.IcosahedronGeometry(0.26, 1);

    positions.forEach((position, index) => {
      const material = new THREE.MeshStandardMaterial({
        color: palette[index],
        roughness: 0.38,
        metalness: 0.18,
        emissive: palette[index],
        emissiveIntensity: 0.12
      });
      const node = new THREE.Mesh(nodeGeometry, material);
      node.position.set(...position);
      node.scale.setScalar(index === 0 ? 1.55 : 1);
      root.add(node);
      nodes.push(node);
    });

    const links = [
      [0, 1], [0, 2], [0, 3], [0, 4], [0, 5], [0, 6],
      [1, 5], [2, 6], [3, 5], [4, 6]
    ];
    const lineMaterial = new THREE.LineBasicMaterial({ color: 0x64aa9a, transparent: true, opacity: 0.32 });
    links.forEach(([from, to]) => {
      const geometry = new THREE.BufferGeometry().setFromPoints([
        nodes[from].position,
        nodes[to].position
      ]);
      root.add(new THREE.Line(geometry, lineMaterial));
    });

    // Particle Dust
    const dustGeometry = new THREE.BufferGeometry();
    const dust = new Float32Array(150 * 3);
    for (let i = 0; i < dust.length; i += 3) {
      dust[i] = (Math.random() - 0.5) * 12;
      dust[i + 1] = (Math.random() - 0.5) * 7;
      dust[i + 2] = (Math.random() - 0.5) * 4;
    }
    dustGeometry.setAttribute('position', new THREE.BufferAttribute(dust, 3));
    const points = new THREE.Points(
      dustGeometry,
      new THREE.PointsMaterial({ color: 0x8abdb1, size: 0.025, transparent: true, opacity: 0.42 })
    );
    root.add(points);

    let pointerX = 0;
    let pointerY = 0;
    let animFrameId;

    const handlePointerMove = (e) => {
      const bounds = canvas.getBoundingClientRect();
      pointerX = ((e.clientX - bounds.left) / bounds.width - 0.5) * 2;
      pointerY = ((e.clientY - bounds.top) / bounds.height - 0.5) * 2;
    };
    canvas.addEventListener('pointermove', handlePointerMove);

    const handleResize = () => {
      const bounds = canvas.getBoundingClientRect();
      if (!bounds.width || !bounds.height) return;
      renderer.setSize(bounds.width, bounds.height, false);
      camera.aspect = bounds.width / bounds.height;
      camera.updateProjectionMatrix();
    };

    const resizeObserver = new ResizeObserver(handleResize);
    resizeObserver.observe(canvas);
    handleResize();

    const animate = (time) => {
      root.rotation.y += 0.0021;
      root.rotation.x += (pointerY * 0.09 - root.rotation.x) * 0.03;
      root.rotation.z += (pointerX * 0.045 - root.rotation.z) * 0.025;

      const targetIdx = selectedVendorId ? (Number(selectedVendorId) % (nodes.length - 1)) + 1 : 1;

      nodes.forEach((node, idx) => {
        const pulse = 1 + Math.sin(time * 0.0017 + idx) * 0.055;
        node.scale.setScalar((idx === targetIdx ? 1.48 : idx === 0 ? 1.55 : 1) * pulse);
      });

      points.rotation.y -= 0.00055;
      renderer.render(scene, camera);
      animFrameId = requestAnimationFrame(animate);
    };

    animFrameId = requestAnimationFrame(animate);

    return () => {
      cancelAnimationFrame(animFrameId);
      canvas.removeEventListener('pointermove', handlePointerMove);
      resizeObserver.disconnect();
      renderer.dispose();
    };
  }, [selectedVendorId]);

  return <canvas ref={mountRef} className="hero-card-canvas" />;
}
